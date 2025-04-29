package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.dto.BBandsPerformance;
import com.takuro_tamura.autofx.domain.indicator.BBands;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
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
public class BBandsService {
    private final Logger log = LoggerFactory.getLogger(BBandsService.class);
    private final CandleService candleService;
    private final OrderService orderService;

    public List<Order> backTest(List<Candle> candles, int n, double k) {
        if (candles.size() <= n) {
            log.debug("Insufficient candles({}) for n({})", candles.size(), n);
            return Collections.emptyList();
        }

        final double[] closePrices = candleService.extractClosePrices(candles);
        final BBands bbands = new BBands(n, k, closePrices);

        final List<Order> orders = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            if (i < n) continue;

            final Order lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;
            final Candle candle = candles.get(i);

            if (bbands.shouldBuy(i, closePrices)) {
                orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
            } else if (bbands.shouldSell(i, closePrices)) {
                orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
            }
        }

        return orders;
    }

    public BBandsPerformance optimize(List<Candle> candles) {
        double performance = 0.0;
        int bestN = 20;
        double bestK = 2.0;

        for (int n = 10; n < 20; n++) {
            for (double k = 1.9; k < 2.1; k += 0.1) {
                final List<Order> orders = backTest(candles, n, k);
                if (CollectionUtils.isEmpty(orders)) {
                    continue;
                }

                final double profit = orderService.accumulateProfit(orders).doubleValue();
                if (performance < profit) {
                    performance = profit;
                    bestN = n;
                    bestK = k;
                }
            }
        }

        return new BBandsPerformance(performance, bestN, bestK);
    }
}
