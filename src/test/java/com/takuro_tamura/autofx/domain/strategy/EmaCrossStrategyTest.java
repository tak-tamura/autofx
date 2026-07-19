package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.strategy.config.StrategyConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EmaCrossStrategyTest {

    @Test
    void calculatesCloseBasedIndicatorsOnlyOnceForMultipleIndexEvaluations() {
        final List<Candle> candles = candles(80);
        final CandleService candleService = spy(new CandleService(
            mock(TradeConfigParameterService.class),
            mock(CandleRepository.class)
        ));
        final EmaCrossStrategy strategy = new EmaCrossStrategy(candleService, config());

        final PreparedStrategy prepared = strategy.prepare(candles);
        prepared.checkTradeSignal(30);
        prepared.checkTradeSignal(40);
        prepared.checkTradeSignal(50);

        verify(candleService, times(1)).extractClosePrices(candles);
    }

    @Test
    void futureCandlesDoNotChangeSignalsAtEarlierIndexes() {
        final List<Candle> allCandles = candles(80);
        final List<Candle> prefix = List.copyOf(allCandles.subList(0, 60));
        final CandleService candleService = new CandleService(
            mock(TradeConfigParameterService.class),
            mock(CandleRepository.class)
        );
        final EmaCrossStrategy strategy = new EmaCrossStrategy(candleService, config());

        final PreparedStrategy prefixStrategy = strategy.prepare(prefix);
        final PreparedStrategy fullStrategy = strategy.prepare(allCandles);

        for (int i = 15; i < prefix.size(); i++) {
            final TradeSignal prefixSignal = prefixStrategy.checkTradeSignal(i);
            assertThat(fullStrategy.checkTradeSignal(i))
                .as("signal at index %s", i)
                .isEqualTo(prefixSignal);
        }
    }

    @Test
    void rejectsIndexOutsidePreparedDataset() {
        final List<Candle> candles = candles(40);
        final CandleService candleService = new CandleService(
            mock(TradeConfigParameterService.class),
            mock(CandleRepository.class)
        );
        final PreparedStrategy prepared = new EmaCrossStrategy(candleService, config()).prepare(candles);

        assertThatIllegalArgumentException().isThrownBy(() -> prepared.checkTradeSignal(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> prepared.checkTradeSignal(candles.size()));
    }

    private StrategyConfig config() {
        return new StrategyConfig(3, 5, 3, 3, 6, 3, 5, 2.0, 3, 10.0);
    }

    private List<Candle> candles(int size) {
        final List<Candle> candles = new ArrayList<>();
        final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int i = 0; i < size; i++) {
            final double close = 100.0 + (i * 0.05) + Math.sin(i * 0.45);
            candles.add(Candle.builder()
                .time(start.plusHours(i))
                .currencyPair(CurrencyPair.USD_JPY)
                .timeFrame(TimeFrame.HOUR)
                .open(new Price(close - 0.05))
                .close(new Price(close))
                .high(new Price(close + 0.30))
                .low(new Price(close - 0.30))
                .build());
        }
        return candles;
    }
}
