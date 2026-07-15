package com.takuro_tamura.autofx.domain.service;

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
    void fixesAtrAndProtectionLevelsAtEntryWithoutReadingLatestCandles() {
        final List<Candle> candles = List.of(
            candle(0, "100", "100.5", "99.5"),
            candle(1, "100", "102", "98"),
            candle(2, "100", "103", "97"),
            candle(3, "100", "120", "80"),
            candle(4, "105", "106", "99"),
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
            config
        );
        final Strategy strategy = mock(Strategy.class);
        when(strategy.checkTradeSignal(eq(candles), anyInt())).thenReturn(TradeSignal.NONE);
        when(strategy.checkTradeSignal(candles, 2)).thenReturn(TradeSignal.BUY);

        final List<Order> orders = backTestService.backTest(
            CurrencyPair.USD_JPY,
            TimeFrame.HOUR,
            6,
            strategy
        );

        assertThat(orders).hasSize(1);
        final Order order = orders.get(0);
        assertThat(order.getProtectionLevels().entryAtr()).isEqualByComparingTo("5");
        assertThat(order.getProtectionLevels().stopPrice().getValue()).isEqualByComparingTo("95.000");
        assertThat(order.getProtectionLevels().takeProfitPrice().getValue()).isEqualByComparingTo("105.000");
        assertThat(order.getCloseDatetime()).isEqualTo(candles.get(4).getTime());
        assertThat(order.getClosePrice().getValue()).isEqualByComparingTo("105");

        verify(candleRepository, times(1))
            .findAllWithLimit(CurrencyPair.USD_JPY, TimeFrame.HOUR, 6);
        verifyNoInteractions(candleService);
        verify(config, times(1)).getAtrPeriod();
        verify(config, times(1)).getStopLimit();
        verify(config, times(1)).getProfitLimit();
        verify(orderService, times(1)).hasOpenPosition(null);
    }

    private Candle candle(int hour, String close, String high, String low) {
        return Candle.builder()
            .time(LocalDateTime.of(2026, 1, 1, hour, 0))
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price(close))
            .close(new Price(close))
            .high(new Price(high))
            .low(new Price(low))
            .build();
    }
}
