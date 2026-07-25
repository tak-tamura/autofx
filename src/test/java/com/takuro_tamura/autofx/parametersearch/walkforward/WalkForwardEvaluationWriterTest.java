package com.takuro_tamura.autofx.parametersearch.walkforward;

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

class WalkForwardEvaluationWriterTest {

    @Test
    void writesImmutableCandidateWindowAndTradeFiles(@TempDir Path directory) throws Exception {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final LocalDateTime outStart = specification.periods().outOfSampleFrom().atStartOfDay();
        final LocalDateTime outEnd = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        final LocalDateTime candidateWindowEnd =
            outStart.plusMonths(specification.walkForwardCriteria().windowMonths());
        final LocalDateTime windowEnd = candidateWindowEnd.isBefore(outEnd) ? candidateWindowEnd : outEnd;
        final BacktestMetrics metrics = metrics(specification.walkForwardCriteria().minimumTradesPerWindow());
        final RankedCandidate selected = new RankedCandidate(
            1, true, true, List.of(),
            new CandidateBacktestEvaluation(
                specification.strategySearchSpace().baseline(),
                new BacktestResult(List.of(), BacktestAssumptions.current()),
                metrics
            )
        );
        final WalkForwardWindowEvaluation window = new WalkForwardWindowEvaluation(
            new WalkForwardWindow(
                1, outStart, windowEnd
            ),
            new BacktestResult(List.of(), BacktestAssumptions.current()),
            metrics
        );
        final WalkForwardEvaluationResult result = new WalkForwardEvaluationResult(
            "fixed-dataset",
            outStart,
            outEnd,
            specification.walkForwardCriteria(),
            List.of(new WalkForwardCandidateEvaluation(
                selected, List.of(window), BigDecimal.ONE, BigDecimal.ONE, true, List.of()
            ))
        );
        final WalkForwardEvaluationWriter writer = new WalkForwardEvaluationWriter();

        final var written = writer.write(directory, result, specification);

        assertThat(Files.readString(written.summaryPath()))
            .contains("datasetId,inSampleRank,passed,rejectionReasons")
            .contains("fixed-dataset,1,true,\"\"")
            .contains(",1,1,1,"
                + specification.walkForwardCriteria().windowMonths() + ','
                + specification.walkForwardCriteria().minimumTradesPerWindow() + ','
                + specification.walkForwardCriteria().minimumProfitableWindowRate().toPlainString() + ','
                + specification.walkForwardCriteria().minimumPositiveAverageRWindowRate().toPlainString() + "\n");
        assertThat(Files.readString(written.windowsPath()))
            .contains("fixed-dataset,1,1," + outStart + ',' + windowEnd + ','
                + specification.walkForwardCriteria().minimumTradesPerWindow());
        assertThat(written.tradesPath()).exists();
        assertThatIllegalStateException().isThrownBy(() -> writer.write(directory, result, specification));
    }

    private BacktestMetrics metrics(int tradeCount) {
        return new BacktestMetrics(
            tradeCount, tradeCount, 0, 0, BigDecimal.ONE, BigDecimal.valueOf(150), BigDecimal.ZERO,
            BigDecimal.valueOf(100), BigDecimal.TEN, BigDecimal.TEN, Optional.of(BigDecimal.valueOf(3)),
            BigDecimal.valueOf(50), 2, 0, List.of(new BigDecimal("0.2")),
            Optional.of(new BigDecimal("0.2")), new BigDecimal("0.1"), BigDecimal.ZERO
        );
    }
}
