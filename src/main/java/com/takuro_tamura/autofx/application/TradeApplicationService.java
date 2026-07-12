package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.calculator.OrderAmountCalculationService;
import com.takuro_tamura.autofx.application.state.TradeStatePortal;
import com.takuro_tamura.autofx.application.strategy.StrategyFactory;
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

@Service
@RequiredArgsConstructor
public class TradeApplicationService {
    private final static int MINIMUM_ORDER_QUANTITY = 10000;

    private final Logger log = LoggerFactory.getLogger(TradeApplicationService.class);
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeConfigParameterService tradeConfigParameterService;
    private final CandleRepository candleRepository;
    private final OrderRepository orderRepository;
    private final StrategyFactory strategyFactory;
    private final OrderAmountCalculationService orderAmountCalculationService;
    private final TradeStatePortal tradeStatePortal;

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

        final Optional<Order> lastOrder = orderRepository.findLatestByCurrencyPairWithLock(targetPair);
        final Candle latestCandle = candles.get(candles.size() - 1);
        if (orderService.shouldCloseOrder(lastOrder.orElse(null), latestCandle.getClose())) {
            lastOrder.ifPresent(orderService::closeOrder);
        }

        if (CollectionUtils.size(candles) < 2) {
            log.info("Not sufficient candles found({}), abort trading", CollectionUtils.size(candles));
            return;
        }

        final TradeSignal signal = strategyFactory.createEmaCrossStrategy().checkTradeSignal(candles, candles.size() - 2);

        if (signal == TradeSignal.BUY) {
            if (orderService.canMakeNewOrder(lastOrder.orElse(null))) {
                makeOrder(OrderSide.BUY, targetPair);
            } else {
                lastOrder.ifPresent(order -> {
                    if (order.getSide() != OrderSide.BUY) {
                        orderService.closeOrder(order);
                    }
                });
            }
        } else if (signal == TradeSignal.SELL) {
            if (orderService.canMakeNewOrder(lastOrder.orElse(null))) {
                makeOrder(OrderSide.SELL, targetPair);
            } else {
                lastOrder.ifPresent(order -> {
                    if (order.getSide() != OrderSide.SELL) {
                        orderService.closeOrder(order);
                    }
                });
            }
        } else {
            log.info("Skip trading this time because of no trading signal");
        }
    }

    private void makeOrder(OrderSide type, CurrencyPair targetPair) {
        final int orderAmount = orderAmountCalculationService.calculateOrderAmount(type, targetPair);

        if (orderAmount < MINIMUM_ORDER_QUANTITY) {
            log.warn("Not enough available amount, cannot make new order");
            return;
        }

        orderService.makeOrder(targetPair, type, orderAmount);
    }
}
