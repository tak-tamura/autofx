package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.calculator.OrderAmountCalculationService;
import com.takuro_tamura.autofx.application.state.TradeStatePortal;
import com.takuro_tamura.autofx.application.validator.TradeSignalValidator;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import com.takuro_tamura.autofx.domain.service.indicator.AtrCalculator;
import com.takuro_tamura.autofx.infrastructure.external.exception.ApiErrorException;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;

@Service
@RequiredArgsConstructor
public class TradeApplicationService {
    private final Logger log = LoggerFactory.getLogger(TradeApplicationService.class);
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeConfigParameterService tradeConfigParameterService;
    private final CandleRepository candleRepository;
    private final OrderRepository orderRepository;
    private final OrderAmountCalculationService orderAmountCalculationService;
    private final TradeStatePortal tradeStatePortal;
    private final TradeSignalValidator tradeSignalValidator;
    private final OrderCachePort orderCachePort;

    @Transactional
    public void trade() {
        if (!tradeStatePortal.isTradingEnabled()) {
            log.info("Trade is disabled");
            return;
        }

        final CurrencyPair targetPair = tradeConfigParameterService.getTargetCurrencyPair();
        final TimeFrame targetTimeFrame = tradeConfigParameterService.getTargetTimeFrame();
        log.info("Start trade, currency: {}, timeframe: {}",
            targetPair,
            targetTimeFrame
        );

        final List<Candle> candles = candleRepository.findAllWithLimit(
            targetPair,
            targetTimeFrame,
            tradeConfigParameterService.getMaxCandleNum()
        );

        if (CollectionUtils.size(candles) < 2) {
            log.info("Not sufficient candles found({}), abort trading", CollectionUtils.size(candles));
            return;
        }

        final Optional<Order> lastOrder = orderRepository.findLatestByCurrencyPairWithLock(targetPair);
        final Candle latestCandle = candles.get(candles.size() - 1);

        // Handle closing current position
        if (tradeSignalValidator.shouldCloseOrder(lastOrder.orElse(null), latestCandle.getClose())) {
            lastOrder.ifPresent(orderService::closeOrder);
        }

        // Generate and process trading signal
        final TradeSignal signal = tradeSignalValidator.generateSignal(candles);

        if (signal == TradeSignal.BUY) {
            if (!tradeSignalValidator.hasOpenPosition(lastOrder.orElse(null))) {
                makeOrder(OrderSide.BUY, targetPair, candles);
            } else if (tradeSignalValidator.isOppositeSignal(signal, lastOrder.orElse(null))) {
                lastOrder.ifPresent(orderService::closeOrder);
            } else {
                log.info("BUY signal received, but holding current position, no action taken");
            }
        } else if (signal == TradeSignal.SELL) {
            if (!tradeSignalValidator.hasOpenPosition(lastOrder.orElse(null))) {
                makeOrder(OrderSide.SELL, targetPair, candles);
            } else if (tradeSignalValidator.isOppositeSignal(signal, lastOrder.orElse(null))) {
                lastOrder.ifPresent(orderService::closeOrder);
            } else {
                log.info("SELL signal received, but holding current position, no action taken");
            }
        } else {
            log.info("Skip trading this time because of no trading signal");
        }
    }

    private void makeOrder(OrderSide type, CurrencyPair targetPair, List<Candle> candles) {
        final int atrPeriod = tradeConfigParameterService.getAtrPeriod();
        if (candles.size() <= atrPeriod) {
            log.warn("Not enough completed candles to calculate ATR, required={}, actual={}", atrPeriod + 1, candles.size());
            return;
        }
        try {
            final BigDecimal atr = AtrCalculator.calculate(candles, candles.size() - 1, atrPeriod);
            final int orderAmount = orderAmountCalculationService.calculateOrderAmount(type, targetPair, atr);
            final Long orderId = orderService.makeOrder(targetPair, type, orderAmount);
            orderCachePort.saveEntryAtr(orderId, atr);
        } catch (IllegalStateException | ApiErrorException e) {
            log.warn("Risk validation rejected new order: symbol={}, side={}, reason={}", targetPair, type, e.getMessage());
        }
    }
}
