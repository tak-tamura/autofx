package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.indicator.AtrCalculator;
import com.takuro_tamura.autofx.domain.strategy.Strategy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackTestService {
    private final Logger log = LoggerFactory.getLogger(BackTestService.class);
    private final CandleService candleService;
    private final OrderService orderService;
    private final CandleRepository candleRepository;
    private final TradeConfigParameterService tradeConfigParameterService;

    public double getBackTestProfitLoss(CurrencyPair currencyPair, TimeFrame timeFrame, int limit, Strategy strategy) {
        final List<Order> orders = backTest(currencyPair, timeFrame, limit, strategy);
        if (CollectionUtils.isEmpty(orders)) {
            return 0.0;
        }

        return orderService.accumulateProfit(orders).doubleValue();
    }

    public List<Order> backTest(CurrencyPair currencyPair, TimeFrame timeFrame, int limit, Strategy strategy) {
        final List<Candle> candles = candleRepository.findAllWithLimit(currencyPair, timeFrame, limit);
        if (CollectionUtils.isEmpty(candles)) {
            return Collections.emptyList();
        }

        final BacktestRiskParameters riskParameters = new BacktestRiskParameters(
            tradeConfigParameterService.getAtrPeriod(),
            tradeConfigParameterService.getStopLimit(),
            tradeConfigParameterService.getProfitLimit()
        );

        final List<Order> orders = new ArrayList<>();
        Order lastOrder;

        for (int i = 1; i < candles.size() - 1; i++) {
            final Candle candle = candles.get(i);
            lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;

            if (orderService.shouldCloseOrder(lastOrder, candle.getClose())) {
                if (lastOrder != null) {
                    lastOrder.close(candle.getTime(), candle.getClose());
                    log.debug("BackTest Close Order at {}, side: {}, price: {}, profit: {}",
                        candle.getTime(),
                        lastOrder.getSide(),
                        candle.getClose(),
                        lastOrder.calculateProfit()
                    );
                    continue;
                }
            }

            final TradeSignal signal = strategy.checkTradeSignal(candles, i);

            final Order openedOrder = switch (signal) {
                case BUY -> handleSignal(OrderSide.BUY, orders, lastOrder, candle, candles, i, riskParameters);
                case SELL -> handleSignal(OrderSide.SELL, orders, lastOrder, candle, candles, i, riskParameters);
                case NONE -> null;
            };

            if (openedOrder != null) {
                log.debug(
                    "BackTest protection fixed at {}, side: {}, ATR: {}, stopPrice: {}, takeProfitPrice: {}",
                    candle.getTime(),
                    openedOrder.getSide(),
                    openedOrder.getProtectionLevels().entryAtr(),
                    openedOrder.getProtectionLevels().stopPrice(),
                    openedOrder.getProtectionLevels().takeProfitPrice()
                );
            }
        }

        return orders;
    }

    private Order handleSignal(
        OrderSide side,
        List<Order> orders,
        Order lastOrder,
        Candle candle,
        List<Candle> candles,
        int evaluationIndex,
        BacktestRiskParameters riskParameters
    ) {
        if (orderService.hasOpenPosition(lastOrder)) {
            orderService.closeBackTestOrderIfOpposite(side, lastOrder, candle);
            return null;
        }
        if (evaluationIndex < riskParameters.atrPeriod()) {
            log.debug(
                "Skip BackTest entry at {} because ATR({}) is not available at index {}",
                candle.getTime(),
                riskParameters.atrPeriod(),
                evaluationIndex
            );
            return null;
        }

        final BigDecimal atr = AtrCalculator.calculate(
            candles,
            evaluationIndex,
            riskParameters.atrPeriod()
        );
        final Order openedOrder = orderService.createBackTestOrder(side, candle);
        openedOrder.fixProtectionLevels(orderService.createProtectionLevels(
            openedOrder,
            atr,
            riskParameters.stopMultiplier(),
            riskParameters.profitMultiplier()
        ));
        orders.add(openedOrder);
        return openedOrder;
    }

    private record BacktestRiskParameters(
        int atrPeriod,
        BigDecimal stopMultiplier,
        BigDecimal profitMultiplier
    ) {
        private BacktestRiskParameters {
            if (atrPeriod <= 0) {
                throw new IllegalArgumentException("ATR period must be greater than zero");
            }
            if (stopMultiplier == null || stopMultiplier.signum() <= 0) {
                throw new IllegalArgumentException("Stop multiplier must be greater than zero");
            }
            if (profitMultiplier == null || profitMultiplier.signum() <= 0) {
                throw new IllegalArgumentException("Profit multiplier must be greater than zero");
            }
        }
    }
}
