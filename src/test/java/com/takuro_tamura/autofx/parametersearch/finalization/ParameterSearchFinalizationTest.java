package com.takuro_tamura.autofx.parametersearch.finalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.parametersearch.config.MarketPriceType;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetMetadata;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleCandidateEvaluation;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleEvaluationResult;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardCandidateEvaluation;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardEvaluationResult;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardRejectionReason;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardWindow;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardWindowEvaluation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ParameterSearchFinalizationTest {

    @Test
    void preservesInSampleOrderAndDoesNotPromoteRejectedCandidate() {
        final var input = input();

        final ParameterSearchFinalResult result = new ParameterSearchFinalizer().assemble(
            input.metadata(), input.outOfSample(), input.walkForward(), input.specification()
        );

        assertThat(result.candidates()).extracting(FinalCandidateAssessment::inSampleRank)
            .containsExactly(1, 2);
        assertThat(result.candidates()).extracting(FinalCandidateAssessment::disposition)
            .containsExactly(CandidateDisposition.PAPER_REVIEW_CANDIDATE, CandidateDisposition.REJECTED);
        assertThat(result.paperReviewCandidates()).extracting(FinalCandidateAssessment::inSampleRank)
            .containsExactly(1);
    }

    @Test
    void rejectsMismatchedDatasetIdentity() {
        final var input = input();
        final HistoricalDatasetMetadata wrongMetadata = metadata("different-dataset");

        assertThatIllegalArgumentException().isThrownBy(() -> new ParameterSearchFinalizer().assemble(
            wrongMetadata, input.outOfSample(), input.walkForward(), input.specification()
        ));
    }

    @Test
    void writesSafeImmutableManifestContainingOnlyPassedCandidates(@TempDir Path directory) throws Exception {
        final var input = input();
        final ParameterSearchFinalResult result = new ParameterSearchFinalizer().assemble(
            input.metadata(), input.outOfSample(), input.walkForward(), input.specification()
        );
        final Clock clock = Clock.fixed(Instant.parse("2026-07-23T00:00:00Z"), ZoneOffset.UTC);
        final ParameterSearchFinalReportWriter writer = new ParameterSearchFinalReportWriter(clock);

        final var written = writer.write(directory, result, input.specification());

        assertThat(Files.readString(written.reviewPath()))
            .contains("PAPER_REVIEW_CANDIDATE")
            .contains("REJECTED")
            .contains("PROFITABLE_WINDOW_RATE_BELOW_MINIMUM");
        final var manifest = new ObjectMapper().registerModule(new JavaTimeModule())
            .readTree(written.manifestPath().toFile());
        assertThat(manifest.get("manualReviewRequired").asBoolean()).isTrue();
        assertThat(manifest.get("liveTradingAllowed").asBoolean()).isFalse();
        assertThat(manifest.get("candidates")).hasSize(1);
        assertThat(manifest.get("candidates").get(0).get("inSampleRank").asInt()).isEqualTo(1);
        assertThat(Files.readString(written.manifestPath()))
            .contains("\"adxThreshold\" : 20")
            .doesNotContain("2E+1");
        assertThatIllegalStateException().isThrownBy(() ->
            writer.write(directory, result, input.specification())
        );
    }

    private FinalizationInput input() {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final LocalDateTime outStart = specification.periods().outOfSampleFrom().atStartOfDay();
        final LocalDateTime outEnd = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        final StrategyParameterSet baseline = specification.strategySearchSpace().baseline();
        final RankedCandidate first = ranked(1, baseline, metrics("500"));
        final RankedCandidate second = ranked(2, baseline, metrics("300"));
        final BacktestResult empty = new BacktestResult(List.of(), BacktestAssumptions.current());
        final OutOfSampleEvaluationResult outOfSample = new OutOfSampleEvaluationResult(
            "fixed-dataset",
            outStart,
            outEnd,
            List.of(
                new OutOfSampleCandidateEvaluation(first, empty, metrics("200")),
                new OutOfSampleCandidateEvaluation(second, empty, metrics("1000"))
            )
        );
        final WalkForwardWindowEvaluation window = new WalkForwardWindowEvaluation(
            new WalkForwardWindow(
                1,
                outStart,
                outStart.plusMonths(specification.walkForwardCriteria().windowMonths()).isBefore(outEnd)
                    ? outStart.plusMonths(specification.walkForwardCriteria().windowMonths())
                    : outEnd
            ),
            empty,
            metrics("100")
        );
        final WalkForwardEvaluationResult walkForward = new WalkForwardEvaluationResult(
            "fixed-dataset",
            outStart,
            outEnd,
            specification.walkForwardCriteria(),
            List.of(
                new WalkForwardCandidateEvaluation(
                    first, List.of(window), BigDecimal.ONE, BigDecimal.ONE, true, List.of()
                ),
                new WalkForwardCandidateEvaluation(
                    second, List.of(window), new BigDecimal("0.5"), new BigDecimal("0.5"), false,
                    List.of(WalkForwardRejectionReason.PROFITABLE_WINDOW_RATE_BELOW_MINIMUM)
                )
            )
        );
        return new FinalizationInput(specification, metadata("fixed-dataset"), outOfSample, walkForward);
    }

    private RankedCandidate ranked(int rank, StrategyParameterSet parameters, BacktestMetrics metrics) {
        return new RankedCandidate(
            rank, true, true, List.of(),
            new CandidateBacktestEvaluation(
                parameters, new BacktestResult(List.of(), BacktestAssumptions.current()), metrics
            )
        );
    }

    private HistoricalDatasetMetadata metadata(String datasetId) {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        return new HistoricalDatasetMetadata(
            datasetId,
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            "GMO_COIN_PUBLIC_API",
            specification.marketData().currencyPair(),
            specification.marketData().timeFrame(),
            specification.marketData().priceType(),
            specification.marketData().timeZone().getId(),
            specification.periods().datasetFrom(),
            specification.periods().datasetTo(),
            specification.periods().datasetFrom().atStartOfDay(),
            specification.periods().datasetTo().atTime(23, 0),
            10_000,
            100,
            0,
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }

    private BacktestMetrics metrics(String netProfit) {
        final BigDecimal net = new BigDecimal(netProfit);
        return new BacktestMetrics(
            40, 22, 18, 0, new BigDecimal("0.55"), net.add(BigDecimal.valueOf(1000)),
            BigDecimal.valueOf(1000), net, BigDecimal.TEN, BigDecimal.TEN,
            Optional.of(new BigDecimal("1.5")), BigDecimal.valueOf(500), 3, 3,
            List.of(new BigDecimal("0.2")), Optional.of(new BigDecimal("0.2")),
            new BigDecimal("0.1"), BigDecimal.ZERO
        );
    }

    private record FinalizationInput(
        com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification specification,
        HistoricalDatasetMetadata metadata,
        OutOfSampleEvaluationResult outOfSample,
        WalkForwardEvaluationResult walkForward
    ) {
    }
}
