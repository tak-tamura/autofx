package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.indicator.IchimokuCloud;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IchimokuService {
    private final Logger log = LoggerFactory.getLogger(IchimokuService.class);
    private final CandleService candleService;
    private final OrderService orderService;

    public List<Order> backTest(List<Candle> candles) {
        if (candles.size() <= 52) {
            log.debug("Insufficient candles({}) for ichimoku", candles.size());
            return Collections.emptyList();
        }

        final double[] closePrices = candleService.extractClosePrices(candles);
        final IchimokuCloud ichimoku = new IchimokuCloud(closePrices);

        final List<Order> orders = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            final Order lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;
            final Candle candle = candles.get(i);

            if (ichimoku.shouldBuy(i, candle.getHigh().getValue().doubleValue(), candle.getLow().getValue().doubleValue())) {
                orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
            } else if (ichimoku.shouldSell(i, candle.getHigh().getValue().doubleValue(), candle.getLow().getValue().doubleValue())) {
                orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
            }
        }

        return orders;
    }

    public double optimize(List<Candle> candles) {
        final List<Order> orders = backTest(candles);
        return orderService.accumulateProfit(orders).doubleValue();
    }
}
