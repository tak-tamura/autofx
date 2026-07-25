package com.takuro_tamura.autofx.parametersearch.execution;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
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
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterCandidateGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InSampleParameterSearchRunnerTest {

    @Test
    void evaluatesEveryCandidateUsingOnlyInSampleCandles() {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final int expectedCandidateCount = specification.strategySearchSpace().candidateCount();
        final LocalDateTime inSampleStart = specification.periods().inSampleFrom().atStartOfDay();
        final LocalDateTime inSampleEnd = specification.periods().inSampleTo().atTime(23, 0);
        final BackTestService backTestService = mock(BackTestService.class);
        when(backTestService.run(any(), any(EmaCrossStrategy.class), any()))
            .thenReturn(new BacktestResult(List.of(), BacktestAssumptions.current()));
        final InSampleParameterSearchRunner runner = new InSampleParameterSearchRunner(
            mock(CandleService.class),
            backTestService,
            new BacktestMetricsCalculator(),
            new StrategyParameterCandidateGenerator()
        );
        final List<Candle> dataset = List.of(
            candle(inSampleStart.minusHours(1)),
            candle(inSampleStart),
            candle(inSampleEnd),
            candle(inSampleEnd.plusHours(1))
        );

        final InSampleParameterSearchResult result = runner.run("fixed-dataset", dataset, specification);

        assertThat(result.datasetId()).isEqualTo("fixed-dataset");
        assertThat(result.evaluations()).hasSize(expectedCandidateCount);
        assertThat(result.evaluations().get(0).parameters()).isEqualTo(specification.strategySearchSpace().baseline());
        assertThat(result.evaluations()).allSatisfy(evaluation -> {
            assertThat(evaluation.backtestResult().trades()).isEmpty();
            assertThat(evaluation.metrics().tradeCount()).isZero();
        });

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Candle>> candlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(backTestService, times(expectedCandidateCount))
            .run(candlesCaptor.capture(), any(EmaCrossStrategy.class), any());
        assertThat(candlesCaptor.getAllValues()).allSatisfy(candles ->
            assertThat(candles).extracting(Candle::getTime).containsExactly(
                inSampleStart,
                inSampleEnd
            )
        );
    }

    @Test
    void rejectsOutOfOrderInSampleCandles() {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final InSampleParameterSearchRunner runner = new InSampleParameterSearchRunner(
            mock(CandleService.class), mock(BackTestService.class),
            new BacktestMetricsCalculator(), new StrategyParameterCandidateGenerator()
        );

        assertThatIllegalArgumentException().isThrownBy(() -> runner.run(
            "fixed-dataset",
            List.of(
                candle(LocalDateTime.of(2024, 1, 1, 1, 0)),
                candle(LocalDateTime.of(2024, 1, 1, 0, 0))
            ),
            specification
        ));
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
