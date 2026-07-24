package com.takuro_tamura.autofx.parametersearch.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestRiskParameters;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.MarketPriceType;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.config.WalkForwardCriteria;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaperCandidatePreparationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC
    );

    @Test
    void preparesOnlyTheExplicitRankAndKeepsAllSafetyFlagsDisabled() {
        final var plan = new PaperCandidatePreparer(FIXED_CLOCK).prepare(
            manifest(true, false, List.of(candidate(2, candidateParameters()))),
            2,
            currentConfiguration()
        );

        assertThat(plan.candidateRank()).isEqualTo(2);
        assertThat(plan.manualReviewRequired()).isTrue();
        assertThat(plan.applyAllowed()).isFalse();
        assertThat(plan.liveTradingAllowed()).isFalse();
        assertThat(plan.differences())
            .filteredOn(PaperCandidatePreparationPlan.ParameterDifference::changed)
            .extracting(PaperCandidatePreparationPlan.ParameterDifference::parameter)
            .containsExactly("emaShortPeriod");
    }

    @Test
    void rejectsUnsafeFlagsUnsupportedSchemaAndImplicitCandidateSelection() {
        final var preparer = new PaperCandidatePreparer(FIXED_CLOCK);
        final var current = currentConfiguration();

        assertThatThrownBy(() -> preparer.prepare(
            manifest(false, false, List.of(candidate(1, candidateParameters()))), 1, current
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("safety flags");
        assertThatThrownBy(() -> preparer.prepare(
            manifest(true, true, List.of(candidate(1, candidateParameters()))), 1, current
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("safety flags");
        final var valid = manifest(true, false, List.of(candidate(1, candidateParameters())));
        final var unsupported = new PaperCandidateManifest(
            2, valid.generatedAt(), valid.manualReviewRequired(), valid.liveTradingAllowed(),
            valid.strategyClass(), valid.dataset(), valid.executionAssumptions(), valid.riskParameters(),
            valid.walkForwardCriteria(), valid.candidates()
        );
        assertThatThrownBy(() -> preparer.prepare(unsupported, 1, current))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported");
        assertThatThrownBy(() -> preparer.prepare(valid, 0, current))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("explicitly specified");
    }

    @Test
    void rejectsDuplicateRanksUnknownRankAndDifferentMarketIdentity() {
        final var preparer = new PaperCandidatePreparer(FIXED_CLOCK);
        final var duplicateRanks = manifest(
            true, false, List.of(candidate(1, candidateParameters()), candidate(1, currentParameters()))
        );
        assertThatThrownBy(() -> preparer.prepare(duplicateRanks, 1, currentConfiguration()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicate");
        assertThatThrownBy(() -> preparer.prepare(
            manifest(true, false, List.of(candidate(1, candidateParameters()))), 2, currentConfiguration()
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not present");
        final var wrongMarket = new PaperCurrentConfiguration(
            CurrencyPair.EUR_USD, TimeFrame.HOUR, 14, currentParameters()
        );
        assertThatThrownBy(() -> preparer.prepare(
            manifest(true, false, List.of(candidate(1, candidateParameters()))), 1, wrongMarket
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("market identity");
    }

    @Test
    void readsInputsAndWritesAnImmutableReviewPlan(@TempDir Path directory) throws Exception {
        final var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        final Path manifestPath = directory.resolve("manifest.json");
        final Path currentPath = directory.resolve("current.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            manifestPath.toFile(), manifest(true, false, List.of(candidate(1, candidateParameters())))
        );
        mapper.writerWithDefaultPrettyPrinter().writeValue(currentPath.toFile(), currentConfiguration());

        final var reader = new PaperCandidatePreparationReader();
        final var plan = new PaperCandidatePreparer(FIXED_CLOCK).prepare(
            reader.readManifest(manifestPath), 1, reader.readCurrentConfiguration(currentPath)
        );
        final var writer = new PaperCandidatePreparationWriter();
        final Path written = writer.write(directory.resolve("output"), plan);

        assertThat(written).exists();
        assertThat(mapper.readTree(written.toFile()).get("applyAllowed").asBoolean()).isFalse();
        assertThat(java.nio.file.Files.readString(written))
            .contains("\"adxThreshold\" : 20")
            .doesNotContain("2E+1");
        assertThatThrownBy(() -> writer.write(directory.resolve("output"), plan))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("immutable");
    }

    private PaperCurrentConfiguration currentConfiguration() {
        return new PaperCurrentConfiguration(CurrencyPair.USD_JPY, TimeFrame.HOUR, 14, currentParameters());
    }

    private StrategyParameterSet currentParameters() {
        return parameters(8);
    }

    private StrategyParameterSet candidateParameters() {
        return parameters(10);
    }

    private StrategyParameterSet parameters(int emaShort) {
        return new StrategyParameterSet(
            emaShort, 21, 14, 12, 26, 9, 20, new BigDecimal("2.0"), 14, new BigDecimal("2E+1")
        );
    }

    private PaperCandidateManifest manifest(
        boolean manualReviewRequired,
        boolean liveTradingAllowed,
        List<PaperCandidateManifest.PaperCandidate> candidates
    ) {
        return new PaperCandidateManifest(
            1,
            FIXED_CLOCK.instant(),
            manualReviewRequired,
            liveTradingAllowed,
            EmaCrossStrategy.class.getName(),
            metadata(),
            BacktestAssumptions.current(),
            new BacktestRiskParameters(14, new BigDecimal("1.5"), new BigDecimal("3.0")),
            new WalkForwardCriteria(3, 5, new BigDecimal("0.75"), new BigDecimal("0.75")),
            candidates
        );
    }

    private PaperCandidateManifest.PaperCandidate candidate(int rank, StrategyParameterSet parameters) {
        final var metrics = new PaperCandidateManifest.MetricSnapshot(
            40, new BigDecimal("0.55"), new BigDecimal("10"), new BigDecimal("1.4"),
            new BigDecimal("2"), 3, new BigDecimal("0.2"), new BigDecimal("0.1"), BigDecimal.ZERO
        );
        return new PaperCandidateManifest.PaperCandidate(
            rank, parameters, metrics, metrics, new BigDecimal("0.75"), new BigDecimal("0.75")
        );
    }

    private HistoricalDatasetMetadata metadata() {
        return new HistoricalDatasetMetadata(
            "dataset-1", "abc123", "GMO_COIN_PUBLIC_API", CurrencyPair.USD_JPY, TimeFrame.HOUR,
            MarketPriceType.ASK, "Asia/Tokyo", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
            LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 0),
            100, 0, 0, FIXED_CLOCK.instant()
        );
    }
}
