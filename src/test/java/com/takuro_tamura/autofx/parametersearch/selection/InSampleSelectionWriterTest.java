package com.takuro_tamura.autofx.parametersearch.selection;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.backtest.BacktestExitReason;
import com.takuro_tamura.autofx.domain.backtest.BacktestTrade;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.ProtectionLevels;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.execution.InSampleParameterSearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class InSampleSelectionWriterTest {

    @Test
    void writesImmutableRankingAndTradeLedgerCsv(@TempDir Path outputDirectory) throws Exception {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final var metrics = new BacktestMetrics(
            30, 20, 10, 0, new BigDecimal("0.66666667"),
            new BigDecimal("300"), new BigDecimal("100"), new BigDecimal("200"),
            new BigDecimal("15"), new BigDecimal("10"), Optional.of(new BigDecimal("3")),
            new BigDecimal("50"), 4, 2, List.of(BigDecimal.ONE), Optional.of(new BigDecimal("0.2")),
            new BigDecimal("0.15"), BigDecimal.ZERO
        );
        final var evaluation = new CandidateBacktestEvaluation(
            specification.strategySearchSpace().baseline(),
            new BacktestResult(List.of(trade()), BacktestAssumptions.current()),
            metrics
        );
        final var searchResult = new InSampleParameterSearchResult(
            "fixed-dataset", LocalDateTime.of(2023, 10, 28, 0, 0),
            LocalDateTime.of(2025, 1, 1, 0, 0), List.of(evaluation)
        );
        final InSampleCandidateSelection selection = new InSampleCandidateRanker()
            .rank(searchResult, specification.selectionCriteria());
        final InSampleSelectionWriter writer = new InSampleSelectionWriter();

        final var written = writer.write(outputDirectory, selection, specification);

        assertThat(written.summaryPath()).exists();
        assertThat(Files.readString(written.summaryPath()))
            .contains("datasetId,periodStart,periodEndExclusive,rank")
            .contains("fixed-dataset,2023-10-28T00:00,2025-01-01T00:00,1,true,true")
            .contains(",0,0,0,14,1.5,3,30,0,1.0,0,5\n");
        assertThat(Files.readString(written.tradesPath()))
            .contains("datasetId,rank,selected,signalTime,fillTime,closeTime,side,size")
            .contains("fixed-dataset,1,true,2024-01-01T00:00,2024-01-01T01:00,2024-01-01T02:00,"
                + "BUY,10000,100,103,2,97,103,TAKE_PROFIT,30000");
        assertThatIllegalStateException().isThrownBy(() -> writer.write(outputDirectory, selection, specification));
    }

    private BacktestTrade trade() {
        final Order order = new Order(
            1L,
            CurrencyPair.USD_JPY,
            OrderSide.BUY,
            10_000,
            LocalDateTime.of(2024, 1, 1, 1, 0),
            new Price("100")
        );
        order.fixProtectionLevels(new ProtectionLevels(
            new BigDecimal("2"), new Price("97"), new Price("103")
        ));
        order.close(LocalDateTime.of(2024, 1, 1, 2, 0), new Price("103"));
        return new BacktestTrade(
            order,
            LocalDateTime.of(2024, 1, 1, 0, 0),
            BacktestExitReason.TAKE_PROFIT
        );
    }
}
