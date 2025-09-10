package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.external.request.CloseOrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.response.OrderResponse;
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
    private PrivateApi privateApi;

    @BeforeEach
    void setUp() {
        candleService = mock(CandleService.class);
        privateApi = mock(PrivateApi.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        TradeConfigParameterService tradeConfigParameterService = mock(TradeConfigParameterService.class);
        when(tradeConfigParameterService.getStopLimit()).thenReturn(BigDecimal.valueOf(1.5));
        when(tradeConfigParameterService.getProfitLimit()).thenReturn(BigDecimal.valueOf(3.0));
        when(tradeConfigParameterService.getAtrPeriod()).thenReturn(7);
        orderService = new OrderService(
            null,
            privateApi,
            candleService,
            redisCacheService,
            tradeConfigParameterService
        );
    }

    @Test
    void testMakeStopAndProfitOrder() {
        // Arrange
        Order mockOrder = mock(Order.class);
        when(mockOrder.getSide()).thenReturn(OrderSide.BUY);
        when(mockOrder.getFillPrice()).thenReturn(new Price(BigDecimal.valueOf(100)));
        when(mockOrder.getCurrencyPair()).thenReturn(CurrencyPair.USD_JPY);
        when(mockOrder.getSize()).thenReturn(10000);

        OrderResponse mockOrderResponse = mock(OrderResponse.class);
        when(mockOrderResponse.getOrderId()).thenReturn(12345L);
        when(candleService.getTR(anyInt())).thenReturn(new double[]{0.21, 0.44, 0.65, 1.12, 0.43, 0.85, 0.19});
        when(privateApi.closeOrder(any(CloseOrderRequest.class)))
            .thenReturn(Collections.singletonList(mockOrderResponse));

        // Act
        orderService.makeStopAndProfitOrder(mockOrder);

        // Assert
        ArgumentCaptor<CloseOrderRequest> captor = ArgumentCaptor.forClass(CloseOrderRequest.class);
        verify(privateApi, times(1)).closeOrder(captor.capture());

        CloseOrderRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest).isNotNull();
        assertThat(capturedRequest.getStopPrice()).isGreaterThan(0);
        assertThat(capturedRequest.getLimitPrice()).isGreaterThan(0);
    }
}