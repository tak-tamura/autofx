package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.dto.MacdPerformance;
import com.takuro_tamura.autofx.domain.indicator.Macd;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class MacdService {
    private final Logger log = LoggerFactory.getLogger(MacdService.class);
    private final CandleService candleService;
    private final OrderService orderService;

    public List<Order> backTest(List<Candle> candles, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (candles.size() <= fastPeriod || candles.size() <= slowPeriod || candles.size() < signalPeriod) {
            log.debug(
                "Insufficient candles({}) for periods(fast:{}, slow:{}, signal:{})",
                candles.size(),
                fastPeriod,
                slowPeriod,
                signalPeriod
            );
        }

        final Macd macd = new Macd(fastPeriod, slowPeriod, signalPeriod, candleService.extractClosePrices(candles));

        final List<Order> orders = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            final Order lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;
            final Candle candle = candles.get(i);

            if (macd.shouldBuy(i)) {
                orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
            } else if (macd.shouldSell(i)) {
                orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
            }
        }

        return orders;
    }

    public MacdPerformance optimize(List<Candle> candles) {
        double performance = 0.0;
        int bestFastPeriod = 12;
        int bestSlowPeriod = 26;
        int bestSignalPeriod = 9;

        for (int fastPeriod = 10; fastPeriod < 19; fastPeriod++) {
            for (int slowPeriod = 20; slowPeriod < 30; slowPeriod++) {
                for (int signalPeriod = 5; signalPeriod < 15; signalPeriod++) {
                    final List<Order> orders = backTest(candles, fastPeriod, slowPeriod, signalPeriod);
                    if (CollectionUtils.isEmpty(orders)) {
                        continue;
                    }

                    final double profit = orderService.accumulateProfit(orders).doubleValue();
                    if (performance < profit) {
                        performance = profit;
                        bestFastPeriod = fastPeriod;
                        bestSlowPeriod = slowPeriod;
                        bestSignalPeriod = signalPeriod;
                    }
                }
            }
        }

        return new MacdPerformance(performance, bestFastPeriod, bestSlowPeriod, bestSignalPeriod);
    }
}
