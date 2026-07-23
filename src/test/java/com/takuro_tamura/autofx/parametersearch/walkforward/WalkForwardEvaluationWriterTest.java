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
        final BacktestMetrics metrics = metrics();
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
                1, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 4, 1, 0, 0)
            ),
            new BacktestResult(List.of(), BacktestAssumptions.current()),
            metrics
        );
        final WalkForwardEvaluationResult result = new WalkForwardEvaluationResult(
            "fixed-dataset",
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 1, 0, 0),
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
            .contains(",1,1,1,3,5,0.75,0.75\n");
        assertThat(Files.readString(written.windowsPath()))
            .contains("fixed-dataset,1,1,2025-01-01T00:00,2025-04-01T00:00,5");
        assertThat(written.tradesPath()).exists();
        assertThatIllegalStateException().isThrownBy(() -> writer.write(directory, result, specification));
    }

    private BacktestMetrics metrics() {
        return new BacktestMetrics(
            5, 3, 2, 0, new BigDecimal("0.6"), BigDecimal.valueOf(150), BigDecimal.valueOf(50),
            BigDecimal.valueOf(100), BigDecimal.TEN, BigDecimal.TEN, Optional.of(BigDecimal.valueOf(3)),
            BigDecimal.valueOf(50), 2, 1, List.of(new BigDecimal("0.2")),
            Optional.of(new BigDecimal("0.2")), new BigDecimal("0.1"), BigDecimal.ZERO
        );
    }
}
