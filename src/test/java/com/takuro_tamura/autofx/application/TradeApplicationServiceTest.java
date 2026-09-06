package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.calculator.OrderAmountCalculationService;
import com.takuro_tamura.autofx.application.state.TradeStatePortal;
import com.takuro_tamura.autofx.application.validator.TradeSignalValidator;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TradeApplicationServiceTest {

    @Test
    void riskRejectionDoesNotReachOrderPlacementPort() {
        final CandleService candleService = mock(CandleService.class);
        final TradeConfigParameterService config = mock(TradeConfigParameterService.class);
        final CandleRepository candleRepository = mock(CandleRepository.class);
        final OrderRepository orderRepository = mock(OrderRepository.class);
        final OrderAmountCalculationService amountCalculationService = mock(OrderAmountCalculationService.class);
        final TradeStatePortal tradeStatePortal = mock(TradeStatePortal.class);
        final TradeSignalValidator tradeSignalValidator = mock(TradeSignalValidator.class);
        final OrderCachePort orderCachePort = mock(OrderCachePort.class);
        final OrderPlacementPort orderPlacementPort = mock(OrderPlacementPort.class);
        final OrderService orderService = new OrderService(
            orderPlacementPort,
            orderCachePort,
            candleService,
            config
        );
        final TradeApplicationService service = new TradeApplicationService(
            candleService,
            orderService,
            config,
            candleRepository,
            orderRepository,
            amountCalculationService,
            tradeStatePortal,
            tradeSignalValidator,
            orderCachePort
        );
        final List<Candle> candles = List.of(
            candle(0, "150.000", "150.100", "149.900"),
            candle(1, "150.000", "150.200", "149.800")
        );

        when(tradeStatePortal.isTradingEnabled()).thenReturn(true);
        when(config.getTargetCurrencyPair()).thenReturn(CurrencyPair.USD_JPY);
        when(config.getTargetTimeFrame()).thenReturn(TimeFrame.HOUR);
        when(config.getMaxCandleNum()).thenReturn(100);
        when(config.getAtrPeriod()).thenReturn(1);
        when(candleRepository.findAllWithLimit(CurrencyPair.USD_JPY, TimeFrame.HOUR, 100))
            .thenReturn(candles);
        when(orderRepository.findLatestByCurrencyPairWithLock(CurrencyPair.USD_JPY))
            .thenReturn(Optional.empty());
        when(tradeSignalValidator.generateSignal(candles)).thenReturn(TradeSignal.BUY);
        when(amountCalculationService.calculateOrderAmount(
            eq(OrderSide.BUY), eq(CurrencyPair.USD_JPY), any(BigDecimal.class)))
            .thenThrow(new IllegalStateException("spread must be less than stop distance"));

        service.trade();

        verify(amountCalculationService).calculateOrderAmount(
            eq(OrderSide.BUY), eq(CurrencyPair.USD_JPY), any(BigDecimal.class));
        verifyNoInteractions(orderPlacementPort);
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
