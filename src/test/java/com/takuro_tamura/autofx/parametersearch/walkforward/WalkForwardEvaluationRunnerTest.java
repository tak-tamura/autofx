package com.takuro_tamura.autofx.parametersearch.walkforward;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetricsCalculator;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleCandidateEvaluation;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleEvaluationResult;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalkForwardEvaluationRunnerTest {

    @Test
    void passesCandidateMeetingEveryPredefinedWindowCriterion() {
        final BacktestMetrics[] windowMetrics = {
            metrics(5, "100", "0.20"),
            metrics(6, "50", "0.10"),
            metrics(7, "20", "0.05"),
            metrics(5, "-10", "-0.02")
        };

        final WalkForwardCandidateEvaluation candidate = run(windowMetrics);

        assertThat(candidate.windows()).extracting(value -> value.window().start())
            .containsExactly(
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 4, 1, 0, 0),
                LocalDateTime.of(2025, 7, 1, 0, 0),
                LocalDateTime.of(2025, 10, 1, 0, 0)
            );
        assertThat(candidate.profitableWindowRate()).isEqualByComparingTo("0.75");
        assertThat(candidate.positiveAverageRWindowRate()).isEqualByComparingTo("0.75");
        assertThat(candidate.passed()).isTrue();
        assertThat(candidate.rejectionReasons()).isEmpty();
    }

    @Test
    void recordsAllFailedWalkForwardCriteria() {
        final WalkForwardCandidateEvaluation candidate = run(new BacktestMetrics[]{
            metrics(4, "100", "0.20"),
            metrics(5, "-10", "-0.10"),
            metrics(5, "-20", "-0.20"),
            metrics(5, "10", "0.10")
        });

        assertThat(candidate.passed()).isFalse();
        assertThat(candidate.profitableWindowRate()).isEqualByComparingTo("0.5");
        assertThat(candidate.positiveAverageRWindowRate()).isEqualByComparingTo("0.5");
        assertThat(candidate.rejectionReasons()).containsExactly(
            WalkForwardRejectionReason.INSUFFICIENT_TRADES_IN_ONE_OR_MORE_WINDOWS,
            WalkForwardRejectionReason.PROFITABLE_WINDOW_RATE_BELOW_MINIMUM,
            WalkForwardRejectionReason.POSITIVE_AVERAGE_R_WINDOW_RATE_BELOW_MINIMUM
        );
    }

    private WalkForwardCandidateEvaluation run(BacktestMetrics[] windowMetrics) {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final RankedCandidate selected = new RankedCandidate(
            1, true, true, List.of(),
            new CandidateBacktestEvaluation(
                specification.strategySearchSpace().baseline(),
                new BacktestResult(List.of(), BacktestAssumptions.current()),
                metrics(40, "500", "0.20")
            )
        );
        final BacktestResult emptyResult = new BacktestResult(List.of(), BacktestAssumptions.current());
        final OutOfSampleEvaluationResult outOfSample = new OutOfSampleEvaluationResult(
            "fixed-dataset",
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 1, 0, 0),
            List.of(new OutOfSampleCandidateEvaluation(selected, emptyResult, metrics(20, "100", "0.10")))
        );
        final BackTestService backTestService = mock(BackTestService.class);
        when(backTestService.run(any(), any(EmaCrossStrategy.class), any(), any(LocalDateTime.class)))
            .thenReturn(emptyResult);
        final BacktestMetricsCalculator calculator = mock(BacktestMetricsCalculator.class);
        when(calculator.calculate(any(), any(), any())).thenReturn(
            windowMetrics[0], windowMetrics[1], windowMetrics[2], windowMetrics[3]
        );
        final WalkForwardEvaluationResult result = new WalkForwardEvaluationRunner(
            mock(CandleService.class), backTestService, calculator
        ).run(dataset(), outOfSample, specification);

        verify(backTestService, times(4)).run(any(), any(EmaCrossStrategy.class), any(), any(LocalDateTime.class));
        return result.candidates().get(0);
    }

    private List<Candle> dataset() {
        return List.of(
            candle(LocalDateTime.of(2024, 12, 31, 23, 0)),
            candle(LocalDateTime.of(2025, 1, 1, 0, 0)),
            candle(LocalDateTime.of(2025, 3, 31, 23, 0)),
            candle(LocalDateTime.of(2025, 4, 1, 0, 0)),
            candle(LocalDateTime.of(2025, 6, 30, 23, 0)),
            candle(LocalDateTime.of(2025, 7, 1, 0, 0)),
            candle(LocalDateTime.of(2025, 9, 30, 23, 0)),
            candle(LocalDateTime.of(2025, 10, 1, 0, 0)),
            candle(LocalDateTime.of(2025, 12, 31, 23, 0))
        );
    }

    private Candle candle(LocalDateTime time) {
        return Candle.builder()
            .time(time).currencyPair(CurrencyPair.USD_JPY).timeFrame(TimeFrame.HOUR)
            .open(new Price("100")).high(new Price("101")).low(new Price("99")).close(new Price("100"))
            .build();
    }

    private BacktestMetrics metrics(int trades, String netProfit, String averageR) {
        final BigDecimal net = new BigDecimal(netProfit);
        final BigDecimal average = new BigDecimal(averageR);
        return new BacktestMetrics(
            trades, trades / 2, trades / 2, trades % 2, new BigDecimal("0.5"),
            net.max(BigDecimal.ZERO).add(BigDecimal.valueOf(100)),
            net.max(BigDecimal.ZERO).add(BigDecimal.valueOf(100)).subtract(net),
            net, BigDecimal.TEN, BigDecimal.TEN, Optional.of(new BigDecimal("1.2")),
            new BigDecimal("50"), 2, 2, List.of(average), Optional.of(average),
            new BigDecimal("0.1"), BigDecimal.ZERO
        );
    }
}
