package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderService orderService;
    private CandleService candleService;
    private OrderPlacementPort orderPlacementPort;
    private OrderCachePort orderCachePort;

    @BeforeEach
    void setUp() {
        candleService = mock(CandleService.class);
        orderPlacementPort = mock(OrderPlacementPort.class);
        orderCachePort = mock(OrderCachePort.class);
        TradeConfigParameterService tradeConfigParameterService = mock(TradeConfigParameterService.class);
        when(tradeConfigParameterService.getStopLimit()).thenReturn(BigDecimal.valueOf(1.5));
        when(tradeConfigParameterService.getProfitLimit()).thenReturn(BigDecimal.valueOf(3.0));
        when(tradeConfigParameterService.getAtrPeriod()).thenReturn(7);
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

        when(candleService.getTR(anyInt())).thenReturn(new double[]{0.21, 0.44, 0.65, 1.12, 0.43, 0.85, 0.19});
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

        assertThat(stopPriceCaptor.getValue()).isGreaterThan(0);
        assertThat(limitPriceCaptor.getValue()).isGreaterThan(0);
        verify(orderCachePort).mapCloseOrderToOriginalOrder(12345L, 67890L);
    }
}
