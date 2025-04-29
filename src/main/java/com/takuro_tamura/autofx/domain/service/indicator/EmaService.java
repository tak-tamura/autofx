package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.dto.EmaPerformance;
import com.takuro_tamura.autofx.domain.indicator.Ema;
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
public class EmaService {

    private final Logger log = LoggerFactory.getLogger(EmaService.class);
    private final CandleService candleService;
    private final OrderService orderService;

    public List<Order> backTest(List<Candle> candles, int period1, int period2) {
        if (candles.size() <= period1 || candles.size() <= period2) {
            log.debug("Insufficient candles({}) for periods[{}, {}]", candles.size(), period1, period2);
            return Collections.emptyList();
        }

        final double[] closePrices = candleService.extractClosePrices(candles);
        final Ema ema = new Ema(new int[]{ period1, period2}, closePrices);

        final List<Order> orders = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            if (i < period1 || i < period2) continue;

            final Order lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;
            final Candle candle = candles.get(i);

            if (ema.shouldBuy(i)) {
                orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
            } else if (ema.shouldSell(i)) {
                orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
            }
        }

        return orders;
    }

    public EmaPerformance optimize(List<Candle> candles) {
        double performance = 0.0;
        int bestPeriod1 = 7;
        int bestPeriod2 = 14;

        for (int period1 = 5; period1 < 50; period1++) {
            for (int period2 = 12; period2 < 50; period2++) {
                final List<Order> orders = backTest(candles, period1, period2);
                if (CollectionUtils.isEmpty(orders)) {
                    continue;
                }

                final double profit = orderService.accumulateProfit(orders).doubleValue();
                if (performance < profit) {
                    performance = profit;
                    bestPeriod1 = period1;
                    bestPeriod2 = period2;
                }
            }
        }

        return new EmaPerformance(performance, bestPeriod1, bestPeriod2);
    }
}
