package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.backtest.BacktestExecutionPolicy;
import com.takuro_tamura.autofx.domain.backtest.BacktestExitReason;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import com.takuro_tamura.autofx.domain.strategy.Strategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BackTestServiceTest {

    @Test
    void fillsAtNextCandleOpenAndUsesSignalCandleAtr() {
        final List<Candle> candles = List.of(
            candle(0, "100", "100.5", "99.5"),
            candle(1, "100", "102", "98"),
            candle(2, "100", "103", "97"),
            candle(3, "101", "104", "97"),
            candle(4, "105", "105.5", "99"),
            candle(5, "105", "105.5", "104.5")
        );
        final CandleRepository candleRepository = mock(CandleRepository.class);
        when(candleRepository.findAllWithLimit(CurrencyPair.USD_JPY, TimeFrame.HOUR, 6))
            .thenReturn(candles);

        final TradeConfigParameterService config = mock(TradeConfigParameterService.class);
        when(config.getAtrPeriod()).thenReturn(2);
        when(config.getStopLimit()).thenReturn(BigDecimal.ONE);
        when(config.getProfitLimit()).thenReturn(BigDecimal.ONE);

        final CandleService candleService = mock(CandleService.class);
        final OrderService orderService = spy(new OrderService(
            mock(OrderPlacementPort.class),
            mock(OrderCachePort.class),
            candleService,
            config
        ));
        final BackTestService backTestService = new BackTestService(
            candleService,
            orderService,
            candleRepository,
            config,
            new BacktestExecutionPolicy()
        );
        final Strategy strategy = mock(Strategy.class);
        when(strategy.checkTradeSignal(eq(candles), anyInt())).thenReturn(TradeSignal.NONE);
        when(strategy.checkTradeSignal(candles, 2)).thenReturn(TradeSignal.BUY);

        final var result = backTestService.run(
            CurrencyPair.USD_JPY,
            TimeFrame.HOUR,
            6,
            strategy
        );

        assertThat(result.trades()).hasSize(1);
        final var trade = result.trades().get(0);
        final Order order = trade.order();
        assertThat(trade.signalDatetime()).isEqualTo(candles.get(2).getTime());
        assertThat(order.getFillDatetime()).isEqualTo(candles.get(3).getTime());
        assertThat(order.getFillPrice().getValue()).isEqualByComparingTo("101");
        assertThat(order.getProtectionLevels().entryAtr()).isEqualByComparingTo("5");
        assertThat(order.getProtectionLevels().stopPrice().getValue()).isEqualByComparingTo("96.000");
        assertThat(order.getProtectionLevels().takeProfitPrice().getValue()).isEqualByComparingTo("106.000");
        assertThat(order.getCloseDatetime()).isEqualTo(candles.get(5).getTime());
        assertThat(order.getClosePrice().getValue()).isEqualByComparingTo("105");
        assertThat(trade.exitReason()).isEqualTo(BacktestExitReason.END_OF_DATA);
        assertThat(result.assumptions().entryTiming()).isEqualTo("NEXT_CANDLE_OPEN");

        verify(candleRepository, times(1))
            .findAllWithLimit(CurrencyPair.USD_JPY, TimeFrame.HOUR, 6);
        verifyNoInteractions(candleService);
        verify(config, times(1)).getAtrPeriod();
        verify(config, times(1)).getStopLimit();
        verify(config, times(1)).getProfitLimit();
    }

    @Test
    void closesOppositeSignalAtFollowingCandleOpenWithoutReversingPosition() {
        final List<Candle> candles = List.of(
            candle(0, "100", "100", "101", "99"),
            candle(1, "100", "100", "101", "99"),
            candle(2, "100", "100", "101", "99"),
            candle(3, "101", "101", "102", "100"),
            candle(4, "102", "102", "103", "101"),
            candle(5, "99", "99", "100", "98"),
            candle(6, "98", "98", "99", "97")
        );
        final CandleRepository candleRepository = mock(CandleRepository.class);
        when(candleRepository.findAllWithLimit(CurrencyPair.USD_JPY, TimeFrame.HOUR, 7))
            .thenReturn(candles);
        final TradeConfigParameterService config = mock(TradeConfigParameterService.class);
        when(config.getAtrPeriod()).thenReturn(2);
        when(config.getStopLimit()).thenReturn(BigDecimal.TEN);
        when(config.getProfitLimit()).thenReturn(BigDecimal.TEN);
        final OrderService orderService = new OrderService(
            mock(OrderPlacementPort.class), mock(OrderCachePort.class), mock(CandleService.class), config
        );
        final BackTestService backTestService = new BackTestService(
            mock(CandleService.class), orderService, candleRepository, config, new BacktestExecutionPolicy()
        );
        final Strategy strategy = mock(Strategy.class);
        when(strategy.checkTradeSignal(eq(candles), anyInt())).thenReturn(TradeSignal.NONE);
        when(strategy.checkTradeSignal(candles, 2)).thenReturn(TradeSignal.BUY);
        when(strategy.checkTradeSignal(candles, 4)).thenReturn(TradeSignal.SELL);

        final var result = backTestService.run(CurrencyPair.USD_JPY, TimeFrame.HOUR, 7, strategy);

        assertThat(result.trades()).hasSize(1);
        final var trade = result.trades().get(0);
        assertThat(trade.order().getFillDatetime()).isEqualTo(candles.get(3).getTime());
        assertThat(trade.order().getFillPrice().getValue()).isEqualByComparingTo("101");
        assertThat(trade.order().getCloseDatetime()).isEqualTo(candles.get(5).getTime());
        assertThat(trade.order().getClosePrice().getValue()).isEqualByComparingTo("99");
        assertThat(trade.exitReason()).isEqualTo(BacktestExitReason.OPPOSITE_SIGNAL);
    }

    private Candle candle(int hour, String close, String high, String low) {
        return candle(hour, close, close, high, low);
    }

    private Candle candle(int hour, String open, String close, String high, String low) {
        return Candle.builder()
            .time(LocalDateTime.of(2026, 1, 1, hour, 0))
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price(open))
            .close(new Price(close))
            .high(new Price(high))
            .low(new Price(low))
            .build();
    }
}
