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
import java.util.ArrayList;
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
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final int windowCount = windowStarts(specification).size();
        final BacktestMetrics[] windowMetrics = new BacktestMetrics[windowCount];
        java.util.Arrays.fill(windowMetrics, metrics(
            specification.walkForwardCriteria().minimumTradesPerWindow(), "100", "0.20"
        ));

        final WalkForwardCandidateEvaluation candidate = run(windowMetrics);

        assertThat(candidate.windows()).extracting(value -> value.window().start())
            .containsExactlyElementsOf(windowStarts(specification));
        assertThat(candidate.profitableWindowRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(candidate.positiveAverageRWindowRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(candidate.passed()).isTrue();
        assertThat(candidate.rejectionReasons()).isEmpty();
    }

    @Test
    void recordsAllFailedWalkForwardCriteria() {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final int windowCount = windowStarts(specification).size();
        final BacktestMetrics[] metrics = new BacktestMetrics[windowCount];
        java.util.Arrays.fill(metrics, metrics(
            specification.walkForwardCriteria().minimumTradesPerWindow(), "-10", "-0.10"
        ));
        metrics[0] = metrics(
            specification.walkForwardCriteria().minimumTradesPerWindow() - 1, "-10", "-0.10"
        );
        final WalkForwardCandidateEvaluation candidate = run(metrics);

        assertThat(candidate.passed()).isFalse();
        assertThat(candidate.profitableWindowRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(candidate.positiveAverageRWindowRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(candidate.rejectionReasons()).containsExactly(
            WalkForwardRejectionReason.INSUFFICIENT_TRADES_IN_ONE_OR_MORE_WINDOWS,
            WalkForwardRejectionReason.PROFITABLE_WINDOW_RATE_BELOW_MINIMUM,
            WalkForwardRejectionReason.POSITIVE_AVERAGE_R_WINDOW_RATE_BELOW_MINIMUM
        );
    }

    private WalkForwardCandidateEvaluation run(BacktestMetrics[] windowMetrics) {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final LocalDateTime outStart = specification.periods().outOfSampleFrom().atStartOfDay();
        final LocalDateTime outEnd = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
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
            outStart,
            outEnd,
            List.of(new OutOfSampleCandidateEvaluation(selected, emptyResult, metrics(20, "100", "0.10")))
        );
        final BackTestService backTestService = mock(BackTestService.class);
        when(backTestService.run(any(), any(EmaCrossStrategy.class), any(), any(LocalDateTime.class)))
            .thenReturn(emptyResult);
        final BacktestMetricsCalculator calculator = mock(BacktestMetricsCalculator.class);
        final java.util.concurrent.atomic.AtomicInteger metricIndex = new java.util.concurrent.atomic.AtomicInteger();
        when(calculator.calculate(any(), any(), any()))
            .thenAnswer(ignored -> windowMetrics[metricIndex.getAndIncrement()]);
        final WalkForwardEvaluationResult result = new WalkForwardEvaluationRunner(
            mock(CandleService.class), backTestService, calculator
        ).run(dataset(specification), outOfSample, specification);

        verify(backTestService, times(windowMetrics.length))
            .run(any(), any(EmaCrossStrategy.class), any(), any(LocalDateTime.class));
        return result.candidates().get(0);
    }

    private List<Candle> dataset(
        com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification specification
    ) {
        final LocalDateTime end = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        final List<Candle> candles = new ArrayList<>();
        candles.add(candle(specification.periods().outOfSampleFrom().atStartOfDay().minusHours(1)));
        for (LocalDateTime start : windowStarts(specification)) {
            final LocalDateTime windowEnd = start
                .plusMonths(specification.walkForwardCriteria().windowMonths())
                .isBefore(end)
                ? start.plusMonths(specification.walkForwardCriteria().windowMonths())
                : end;
            candles.add(candle(start));
            candles.add(candle(windowEnd.minusHours(1)));
        }
        return candles.stream().distinct().sorted(java.util.Comparator.comparing(Candle::getTime)).toList();
    }

    private List<LocalDateTime> windowStarts(
        com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification specification
    ) {
        final List<LocalDateTime> starts = new ArrayList<>();
        final LocalDateTime end = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        for (LocalDateTime start = specification.periods().outOfSampleFrom().atStartOfDay();
             start.isBefore(end);
             start = start.plusMonths(specification.walkForwardCriteria().windowMonths())) {
            starts.add(start);
        }
        return starts;
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
