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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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

        verifyNoInteractions(privateApi);
    }

    @Test
    void allowsNewOrderWhenSpreadIsLessThanStopDistance() {
        when(config.getStopLimit()).thenReturn(new BigDecimal("2.0"));

        final int quantity = service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.011"));

        assertThat(quantity).isGreaterThanOrEqualTo(OrderAmountCalculationService.MINIMUM_ORDER_QUANTITY);
    }

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    void rejectsBothBuyAndSellWhenSpreadEqualsStopDistance(OrderSide side) {
        when(config.getStopLimit()).thenReturn(new BigDecimal("2.0"));

        assertThatThrownBy(() -> service.calculateOrderAmount(
            side, CurrencyPair.USD_JPY, new BigDecimal("0.010")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("less than stop distance");

        verifyNoInteractions(privateApi);
    }

    @Test
    void rejectsNewOrderWhenSpreadIsGreaterThanStopDistance() {
        when(config.getStopLimit()).thenReturn(BigDecimal.ONE);

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.010")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("less than stop distance");

        verifyNoInteractions(privateApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.1"})
    void rejectsNonPositiveAtr(String atr) {
        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal(atr)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ATR");

        verifyNoInteractions(publicApi, privateApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.5"})
    void rejectsNonPositiveStopMultiplier(String stopMultiplier) {
        when(config.getStopLimit()).thenReturn(new BigDecimal(stopMultiplier));

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.50")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stop multiplier");

        verifyNoInteractions(publicApi, privateApi);
    }

    @Test
    void rejectsMissingAtr() {
        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ATR");

        verifyNoInteractions(publicApi, privateApi);
    }

    @Test
    void rejectsMissingStopMultiplier() {
        when(config.getStopLimit()).thenReturn(null);

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.50")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stop multiplier");

        verifyNoInteractions(publicApi, privateApi);
    }

    @Test
    void rejectsInvalidAsk() {
        setTicker(null, "150.000");

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("0.50")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ask");

        verifyNoInteractions(privateApi);
    }

    @Test
    void rejectsInvalidBid() {
        setTicker("150.020", "not-a-price");

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.SELL, CurrencyPair.USD_JPY, new BigDecimal("0.50")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bid");

        verifyNoInteractions(privateApi);
    }

    @Test
    void rejectsQuantityBelowBrokerMinimum() {
        when(config.getRiskPerTradeRate()).thenReturn(new BigDecimal("0.001"));

        assertThatThrownBy(() -> service.calculateOrderAmount(
            OrderSide.BUY, CurrencyPair.USD_JPY, new BigDecimal("1.0")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("broker minimum");
    }

    private void setTicker(String ask, String bid) {
        final Ticker ticker = new Ticker();
        ticker.setSymbol(CurrencyPair.USD_JPY);
        ticker.setAsk(ask);
        ticker.setBid(bid);
        when(publicApi.getTickers()).thenReturn(List.of(ticker));
    }
}
