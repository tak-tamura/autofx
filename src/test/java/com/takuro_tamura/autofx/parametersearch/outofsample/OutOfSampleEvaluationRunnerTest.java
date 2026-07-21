package com.takuro_tamura.autofx.parametersearch.outofsample;

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
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.selection.InSampleCandidateSelection;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutOfSampleEvaluationRunnerTest {

    @Test
    void evaluatesOnlySelectedCandidatesWithoutChangingInSampleOrder() {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final StrategyParameterSet baseline = specification.strategySearchSpace().baseline();
        final RankedCandidate first = ranked(1, true, baseline, metrics("0.30", "100"));
        final RankedCandidate second = ranked(2, true, withEmaShort(baseline, 7), metrics("0.20", "1000"));
        final RankedCandidate notSelected = ranked(3, false, withEmaShort(baseline, 6), metrics("0.10", "50"));
        final InSampleCandidateSelection selection = new InSampleCandidateSelection(
            "fixed-dataset",
            LocalDateTime.of(2023, 10, 28, 0, 0),
            LocalDateTime.of(2025, 1, 1, 0, 0),
            specification.selectionCriteria(),
            List.of(first, second, notSelected)
        );
        final BacktestResult firstResult = new BacktestResult(List.of(), BacktestAssumptions.current());
        final BacktestResult secondResult = new BacktestResult(List.of(), BacktestAssumptions.current());
        final BackTestService backTestService = mock(BackTestService.class);
        when(backTestService.run(any(), any(EmaCrossStrategy.class), any(), any(LocalDateTime.class)))
            .thenReturn(firstResult, secondResult);
        final BacktestMetricsCalculator calculator = mock(BacktestMetricsCalculator.class);
        when(calculator.calculate(any(), any(), any()))
            .thenReturn(metrics("-0.10", "-500"), metrics("0.50", "2000"));
        final OutOfSampleEvaluationRunner runner = new OutOfSampleEvaluationRunner(
            mock(CandleService.class), backTestService, calculator
        );
        final List<Candle> dataset = List.of(
            candle(LocalDateTime.of(2024, 12, 31, 23, 0)),
            candle(LocalDateTime.of(2025, 1, 1, 0, 0)),
            candle(LocalDateTime.of(2025, 12, 31, 23, 0)),
            candle(LocalDateTime.of(2026, 1, 1, 0, 0))
        );

        final OutOfSampleEvaluationResult result = runner.run(dataset, selection, specification);

        assertThat(result.datasetId()).isEqualTo("fixed-dataset");
        assertThat(result.evaluations()).extracting(OutOfSampleCandidateEvaluation::selectedCandidate)
            .containsExactly(first, second);
        // 2位候補のOOS成績が良くても、Phase 6で固定した1位・2位の順序は変更しない。
        assertThat(result.evaluations()).extracting(evaluation -> evaluation.metrics().netProfit())
            .containsExactly(new BigDecimal("-500"), new BigDecimal("2000"));

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Candle>> candlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(backTestService, times(2)).run(
            candlesCaptor.capture(), any(EmaCrossStrategy.class), any(),
            org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2025, 1, 1, 0, 0))
        );
        assertThat(candlesCaptor.getAllValues()).allSatisfy(candles ->
            assertThat(candles).extracting(Candle::getTime).containsExactly(
                LocalDateTime.of(2024, 12, 31, 23, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 0)
            )
        );
        verify(backTestService, never()).run(any(CurrencyPair.class), any(TimeFrame.class), any(Integer.class), any());
    }

    private RankedCandidate ranked(
        int rank,
        boolean selected,
        StrategyParameterSet parameters,
        BacktestMetrics metrics
    ) {
        return new RankedCandidate(
            rank,
            true,
            selected,
            List.of(),
            new CandidateBacktestEvaluation(
                parameters,
                new BacktestResult(List.of(), BacktestAssumptions.current()),
                metrics
            )
        );
    }

    private BacktestMetrics metrics(String averageR, String netProfit) {
        final BigDecimal average = new BigDecimal(averageR);
        final BigDecimal net = new BigDecimal(netProfit);
        return new BacktestMetrics(
            40, 20, 20, 0, new BigDecimal("0.5"),
            net.max(BigDecimal.ZERO).add(BigDecimal.valueOf(1000)),
            net.max(BigDecimal.ZERO).add(BigDecimal.valueOf(1000)).subtract(net),
            net, BigDecimal.TEN, BigDecimal.TEN, Optional.of(new BigDecimal("1.5")),
            new BigDecimal("500"), 3, 3, List.of(average), Optional.of(average),
            new BigDecimal("0.1"), BigDecimal.ZERO
        );
    }

    private StrategyParameterSet withEmaShort(StrategyParameterSet source, int value) {
        return new StrategyParameterSet(
            value, source.emaLongPeriod(), source.rsiPeriod(), source.macdFastPeriod(),
            source.macdSlowPeriod(), source.macdSignalPeriod(), source.bBandsPeriod(),
            source.bBandsMultiplier(), source.adxPeriod(), source.adxThreshold()
        );
    }

    private Candle candle(LocalDateTime time) {
        return Candle.builder()
            .time(time)
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price("100"))
            .high(new Price("101"))
            .low(new Price("99"))
            .close(new Price("100"))
            .build();
    }
}
