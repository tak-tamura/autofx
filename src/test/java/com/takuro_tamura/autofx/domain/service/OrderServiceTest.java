package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderService orderService;
    private CandleService candleService;
    private OrderPlacementPort orderPlacementPort;
    private OrderCachePort orderCachePort;
    private TradeConfigParameterService tradeConfigParameterService;

    @BeforeEach
    void setUp() {
        candleService = mock(CandleService.class);
        orderPlacementPort = mock(OrderPlacementPort.class);
        orderCachePort = mock(OrderCachePort.class);
        tradeConfigParameterService = mock(TradeConfigParameterService.class);
        when(tradeConfigParameterService.getStopLimit()).thenReturn(BigDecimal.valueOf(1.5));
        when(tradeConfigParameterService.getProfitLimit()).thenReturn(BigDecimal.valueOf(3.0));
        when(tradeConfigParameterService.getAtrPeriod()).thenReturn(2);
        orderService = new OrderService(
            orderPlacementPort,
            orderCachePort,
            candleService,
            tradeConfigParameterService
        );
    }

    @Test
    void testMakeStopAndProfitOrder() {
        // Arrange
        Order mockOrder = mock(Order.class);
        when(mockOrder.getOrderId()).thenReturn(67890L);
        when(mockOrder.getSide()).thenReturn(OrderSide.BUY);
        when(mockOrder.getFillPrice()).thenReturn(new Price(BigDecimal.valueOf(100)));
        when(mockOrder.getCurrencyPair()).thenReturn(CurrencyPair.USD_JPY);
        when(mockOrder.getSize()).thenReturn(10000);

        when(candleService.getLatestCandles(3)).thenReturn(List.of(
            candle(0, "100.0", "100.1", "99.9"),
            candle(1, "100.0", "100.4", "99.8"),
            candle(2, "100.0", "100.6", "99.6")
        ));
        when(orderPlacementPort.submitOcoOrder(eq(mockOrder), anyDouble(), anyDouble()))
            .thenReturn(Collections.singletonList(12345L));

        // Act
        orderService.makeStopAndProfitOrder(mockOrder);

        // Assert
        ArgumentCaptor<Double> stopPriceCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> limitPriceCaptor = ArgumentCaptor.forClass(Double.class);
        verify(orderPlacementPort).submitOcoOrder(
            eq(mockOrder),
            stopPriceCaptor.capture(),
            limitPriceCaptor.capture()
        );

        assertThat(stopPriceCaptor.getValue()).isEqualTo(98.8);
        assertThat(limitPriceCaptor.getValue()).isEqualTo(102.4);
        verify(orderCachePort).mapCloseOrderToOriginalOrder(12345L, 67890L);
    }

    @Test
    void usesProtectionLevelsFixedAtEntryWithoutRecalculatingAtr() {
        final Candle entryCandle = candle(0, "100.0", "100.1", "99.9");
        final Order order = orderService.createDummyOrder(entryCandle, OrderSide.BUY, entryCandle.getClose());
        order.fixProtectionLevels(orderService.createProtectionLevels(
            order,
            new BigDecimal("2.0"),
            BigDecimal.ONE,
            BigDecimal.ONE
        ));

        assertThat(orderService.shouldCloseOrder(order, new Price("101.999"))).isFalse();
        assertThat(orderService.shouldCloseOrder(order, new Price("102.000"))).isTrue();
        assertThatIllegalStateException()
            .isThrownBy(() -> order.fixProtectionLevels(order.getProtectionLevels()));
        verifyNoInteractions(candleService);
        verify(tradeConfigParameterService, never()).getAtrPeriod();
    }

    @Test
    void createsProtectionLevelsForSellWithExplicitRounding() {
        final Candle entryCandle = candle(0, "100.0", "100.1", "99.9");
        final Order order = orderService.createDummyOrder(entryCandle, OrderSide.SELL, entryCandle.getClose());

        final var protectionLevels = orderService.createProtectionLevels(
            order,
            new BigDecimal("0.5555"),
            new BigDecimal("1.5"),
            new BigDecimal("3.0")
        );

        assertThat(protectionLevels.stopPrice().getValue()).isEqualByComparingTo("100.833");
        assertThat(protectionLevels.takeProfitPrice().getValue()).isEqualByComparingTo("98.333");
    }

    @Test
    void detectsOnlyUncompletedOrderAsOpenPosition() {
        final Candle entryCandle = candle(0, "100.0", "100.1", "99.9");
        final Order order = orderService.createDummyOrder(entryCandle, OrderSide.BUY, entryCandle.getClose());

        assertThat(orderService.hasOpenPosition(null)).isFalse();
        assertThat(orderService.hasOpenPosition(order)).isTrue();

        order.close(entryCandle.getTime().plusHours(1), new Price("101.0"));

        assertThat(orderService.hasOpenPosition(order)).isFalse();
    }

    @Test
    void createsAndClosesBackTestOrderWithoutManagingOrderHistory() {
        final Candle entryCandle = candle(0, "100.0", "100.1", "99.9");
        final Order order = orderService.createBackTestOrder(OrderSide.BUY, entryCandle);

        assertThat(orderService.hasOpenPosition(order)).isTrue();

        orderService.closeBackTestOrderIfOpposite(
            OrderSide.SELL,
            order,
            candle(1, "101.0", "101.1", "100.9")
        );

        assertThat(orderService.hasOpenPosition(order)).isFalse();
        assertThat(order.getClosePrice().getValue()).isEqualByComparingTo("101.0");
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
