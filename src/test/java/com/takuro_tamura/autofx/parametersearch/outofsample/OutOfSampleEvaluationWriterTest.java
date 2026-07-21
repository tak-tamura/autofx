package com.takuro_tamura.autofx.parametersearch.outofsample;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;
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

class OutOfSampleEvaluationWriterTest {

    @Test
    void writesImmutableInSampleComparisonAndOutOfSampleLedger(@TempDir Path directory) throws Exception {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final BacktestMetrics inSample = metrics(40, "1000", "1.5", "0.30", "500");
        final BacktestMetrics outOfSample = metrics(30, "200", "1.1", "0.10", "800");
        final RankedCandidate selected = new RankedCandidate(
            1, true, true, List.of(),
            new CandidateBacktestEvaluation(
                specification.strategySearchSpace().baseline(),
                new BacktestResult(List.of(), BacktestAssumptions.current()),
                inSample
            )
        );
        final OutOfSampleEvaluationResult result = new OutOfSampleEvaluationResult(
            "fixed-dataset",
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 1, 0, 0),
            List.of(new OutOfSampleCandidateEvaluation(
                selected,
                new BacktestResult(List.of(), BacktestAssumptions.current()),
                outOfSample
            ))
        );
        final OutOfSampleEvaluationWriter writer = new OutOfSampleEvaluationWriter();

        final var written = writer.write(directory, result, specification);

        assertThat(Files.readString(written.summaryPath()))
            .contains("datasetId,inSampleFrom,inSampleTo,outOfSampleFrom,outOfSampleTo,inSampleRank")
            .contains("fixed-dataset,2023-10-28,2024-12-31,2025-01-01,2025-12-31,1")
            .contains(",40,30,-10,")
            .contains(",1000,200,-800,1.5,1.1,-0.4,500,800,300,0.30,0.10,-0.20,");
        assertThat(Files.readString(written.tradesPath()))
            .isEqualTo("datasetId,inSampleRank,signalTime,fillTime,closeTime,side,size,fillPrice,closePrice,"
                + "entryAtr,stopPrice,takeProfitPrice,exitReason,profit\n");
        assertThatIllegalStateException().isThrownBy(() -> writer.write(directory, result, specification));
    }

    private BacktestMetrics metrics(
        int trades,
        String netProfit,
        String profitFactor,
        String averageR,
        String drawdown
    ) {
        final BigDecimal net = new BigDecimal(netProfit);
        final BigDecimal average = new BigDecimal(averageR);
        return new BacktestMetrics(
            trades, trades / 2, trades / 2, 0, new BigDecimal("0.5"),
            net.add(BigDecimal.valueOf(1000)), BigDecimal.valueOf(1000), net,
            BigDecimal.TEN, BigDecimal.TEN, Optional.of(new BigDecimal(profitFactor)),
            new BigDecimal(drawdown), 3, 3, List.of(average), Optional.of(average),
            new BigDecimal("0.1"), BigDecimal.ZERO
        );
    }
}
