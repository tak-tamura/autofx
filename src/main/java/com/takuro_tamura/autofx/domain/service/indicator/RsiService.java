package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.dto.RsiPerformance;
import com.takuro_tamura.autofx.domain.indicator.Rsi;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RsiService {
    private final static int BUY_THREAD = 30; // TODO consider define in property
    private final static int SELL_THREAD = 70; // TODO consider define in property

    private final CandleService candleService;
    private final OrderService orderService;

    public List<Order> backTest(List<Candle> candles, int period) {
        if (candles.size() <= period) {
            return Collections.emptyList();
        }

        final double[] closePrices = candleService.extractClosePrices(candles);
        final Rsi rsi = new Rsi(period, closePrices);

        final List<Order> orders = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            final Order lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;
            final Candle candle = candles.get(i);

            if (rsi.shouldBuy(i, BUY_THREAD)) {
                orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
            } else if (rsi.shouldSell(i, SELL_THREAD)) {
                orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
            }
        }

        return orders;
    }

    public RsiPerformance optimize(List<Candle> candles) {
        double performance = 0.0;
        int bestPeriod = 14;

        for (int period = 5; period < 25; period++) {
            final List<Order> orders = backTest(candles, period);
            double profit = orderService.accumulateProfit(orders).doubleValue();
            if (performance < profit) {
                performance = profit;
                bestPeriod = period;
            }
        }

        return new RsiPerformance(performance, bestPeriod, BUY_THREAD, SELL_THREAD);
    }
}
