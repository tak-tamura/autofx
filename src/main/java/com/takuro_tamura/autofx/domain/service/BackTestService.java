package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.strategy.Strategy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final Strategy strategy;

    public double getBackTestProfitLoss(CurrencyPair currencyPair, TimeFrame timeFrame, int limit) {
        final List<Order> orders = backTest(currencyPair, timeFrame, limit);
        if (CollectionUtils.isEmpty(orders)) {
            return 0.0;
        }

        return orderService.accumulateProfit(orders).doubleValue();
    }

    public List<Order> backTest(CurrencyPair currencyPair, TimeFrame timeFrame, int limit) {
        final List<Candle> candles = candleRepository.findAllWithLimit(currencyPair, timeFrame, limit);
        if (CollectionUtils.isEmpty(candles)) {
            return Collections.emptyList();
        }

        final double[] closePrices = candleService.extractClosePrices(candles);
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

            final TradeSignal signal = strategy.checkTradeSignal(closePrices, i);

            switch (signal) {
                case BUY -> orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
                case SELL -> orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
                case NONE -> {}
            }
        }

        return orders;
    }
}
