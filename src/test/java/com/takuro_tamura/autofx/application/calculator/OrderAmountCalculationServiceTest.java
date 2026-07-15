package com.takuro_tamura.autofx.application.calculator;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.infrastructure.external.response.Assets;
import com.takuro_tamura.autofx.infrastructure.external.response.OpenPositions;
import com.takuro_tamura.autofx.infrastructure.external.response.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderAmountCalculationServiceTest {
    private PublicApi publicApi;
    private PrivateApi privateApi;
    private TradeConfigParameterService config;
    private OrderAmountCalculationService service;

    @BeforeEach
    void setUp() {
        publicApi = mock(PublicApi.class);
        privateApi = mock(PrivateApi.class);
        config = mock(TradeConfigParameterService.class);
        service = new OrderAmountCalculationService(publicApi, privateApi, config);

        final Ticker ticker = new Ticker();
        ticker.setSymbol(CurrencyPair.USD_JPY);
        ticker.setAsk("150.020");
        ticker.setBid("150.000");
        when(publicApi.getTickers()).thenReturn(List.of(ticker));

        final Assets assets = new Assets();
        assets.setEquity(1_000_000d);
        assets.setAvailableAmount(1_000_000d);
        when(privateApi.getAssets()).thenReturn(assets);
        when(privateApi.getOpenPositions(CurrencyPair.USD_JPY)).thenReturn(new OpenPositions());

        when(config.getRiskPerTradeRate()).thenReturn(new BigDecimal("0.01"));
        when(config.getStopLimit()).thenReturn(new BigDecimal("1.5"));
        when(config.getLeverage()).thenReturn(new BigDecimal("15"));
        when(config.getAvailableBalanceRate()).thenReturn(new BigDecimal("0.8"));
        when(config.getMaxOrderQuantity()).thenReturn(500000);
        when(config.getMaxSpread()).thenReturn(new BigDecimal("0.1"));
    }

    @Test
    void keepsExpectedStopLossWithinConfiguredRisk() {
        final BigDecimal atr = new BigDecimal("0.50");

        final int quantity = service.calculateOrderAmount(OrderSide.BUY, CurrencyPair.USD_JPY, atr);

        final BigDecimal expectedLoss = BigDecimal.valueOf(quantity)
            .multiply(atr.multiply(new BigDecimal("1.5")).add(new BigDecimal("0.020")));
        assertThat(quantity).isEqualTo(12987);
        assertThat(expectedLoss).isLessThanOrEqualTo(new BigDecimal("10000"));
    }

    @Test
    void reducesQuantityWhenAtrIncreases() {
        final int lowVolatility = service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.50"));
        final int highVolatility = service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.60"));

        assertThat(highVolatility).isLessThan(lowVolatility);
    }

    @Test
    void rejectsSpreadAboveConfiguredMaximum() {
        when(config.getMaxSpread()).thenReturn(new BigDecimal("0.01"));

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.SELL, CurrencyPair.USD_JPY, new BigDecimal("0.50")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("spread");
    }

    @Test
    void rejectsQuantityBelowBrokerMinimum() {
        when(config.getRiskPerTradeRate()).thenReturn(new BigDecimal("0.001"));

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("1.0")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("broker minimum");
    }
}
