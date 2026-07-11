package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderPlacementPort orderPlacementPort;
    private final OrderCachePort orderCachePort;
    private final CandleService candleService;
    private final TradeConfigParameterService tradeConfigParameterService;

    public boolean shouldCloseOrder(Order order, Price currentPrice) {
        if (order == null || order.getStatus() != OrderStatus.FILLED) {
            return false;
        }

        // 利益確定・損切りラインに達していたらclose
        final BigDecimal atr = calculateAtr();

        final double stopPrice = calculateStopPrice(
            order.getFillPrice().getValue(),
            tradeConfigParameterService.getStopLimit(),
            atr,
            order.getSide()
        );

        final double limitPrice = calculateLimitPrice(
            order.getFillPrice().getValue(),
            tradeConfigParameterService.getProfitLimit(),
            atr,
            order.getSide()
        );

        if (order.getSide() == OrderSide.BUY) {
            // 利益確定
            if (currentPrice.getValue().doubleValue() >= limitPrice) {
                return true;
            }
            // 損切り
            return currentPrice.getValue().doubleValue() <= stopPrice;
        } else {
            // 利益確定
            if (currentPrice.getValue().doubleValue() <= limitPrice) {
                return true;
            }
            // 損切り
            return currentPrice.getValue().doubleValue() >= stopPrice;
        }
    }

    public boolean canMakeNewOrder(Order lastOrder) {
        return lastOrder == null || lastOrder.getStatus().isCompleted();
    }

    /**
     * 新規オーダー
     * @param currencyPair
     * @param type
     * @param size
     */
    public void makeOrder(CurrencyPair currencyPair, OrderSide type, int size) {
        log.info("Sending new order request: currencyPair={}, side={}, size={}", currencyPair, type, size);
        orderPlacementPort.submitMarketOrder(currencyPair, type, size);
        log.info("Sent order request successfully");
    }

    /**
     * 決済オーダー
     * @param order
     */
    public void closeOrder(Order order) {
        log.info("Sending close order request: {}", order);
        
        Long closeOrderId = orderPlacementPort.submitMarketCloseOrder(order);
        log.info("Sent close order request, closeOrderId: {}", closeOrderId);
        
        // 約定時にオーダー情報を更新するため、決済注文のオーダーIDをキーにして決済対象のオーダーのオーダーIDをキャッシュに保存しておく
        orderCachePort.mapCloseOrderToOriginalOrder(closeOrderId, order.getOrderId());
        log.info("Store orderId({}) with closeOrderId({})", order.getOrderId(), closeOrderId);
    }

    public void makeStopAndProfitOrder(Order order) {
        final BigDecimal atr = calculateAtr();
        log.info("Calculated ATR: {}", atr.doubleValue());

        final double stopPrice = calculateStopPrice(
            order.getFillPrice().getValue(),
            tradeConfigParameterService.getStopLimit(),
            atr,
            order.getSide()
        );
        log.info("Calculated stopPrice: {}", stopPrice);

        final double limitPrice = calculateLimitPrice(
            order.getFillPrice().getValue(),
            tradeConfigParameterService.getProfitLimit(),
            atr,
            order.getSide()
        );
        log.info("Calculated limitPrice: {}", limitPrice);

        List<Long> closeOrderIds = orderPlacementPort.submitOcoOrder(order, stopPrice, limitPrice);
        log.info("Sent OCO order request, orderId: {}", order.getOrderId());

        // 約定時にオーダー情報を更新するため、決済注文のオーダーIDをキーにして決済対象のオーダーのオーダーIDをキャッシュに保存しておく
        closeOrderIds.forEach(closeOrderId ->
            orderCachePort.mapCloseOrderToOriginalOrder(closeOrderId, order.getOrderId())
        );
        log.info("Stored {} close order mappings for original orderId({})", closeOrderIds.size(), order.getOrderId());
    }

    /**
     * リスト内のオーダーすべての利益を集計
     * @param orders
     * @return
     */
    public BigDecimal accumulateProfit(List<Order> orders) {
        return orders.stream()
            .map(Order::calculateProfit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void handleBackTestOrder(OrderSide side, List<Order> orders, Order lastOrder, Candle candle) {
        if (canMakeNewOrder(lastOrder)) {
            final Order order = createDummyOrder(candle, side, candle.getClose());
            orders.add(order);
            log.debug("Make new order at {}, side: {}, price: {}", candle.getTime(), side, candle.getClose());
        } else {
            if (lastOrder != null && lastOrder.getSide() != side) {
                lastOrder.close(candle.getTime(), candle.getClose());
                log.debug("Close order at {}, side: {}, price: {}, profit: {}",
                    candle.getTime(),
                    lastOrder.getSide(),
                    candle.getClose(),
                    lastOrder.calculateProfit()
                );
            }
        }
    }

    public Order createDummyOrder(Candle candle, OrderSide type, Price price) {
        return new Order(
            0L,
            candle.getCurrencyPair(),
            type,
            10000,
            candle.getTime(),
            price
        );
    }

    private double calculateStopPrice(BigDecimal price, BigDecimal limit, BigDecimal atr, OrderSide side) {
        final BigDecimal raw = (side == OrderSide.BUY)
            ? price.subtract(limit.multiply(atr))
            : price.add(limit.multiply(atr));

        final BigDecimal scaled = raw.setScale(3, RoundingMode.DOWN);

        return scaled.doubleValue();
    }

    private double calculateLimitPrice(BigDecimal price, BigDecimal limit, BigDecimal atr, OrderSide side) {
        final BigDecimal raw = (side == OrderSide.BUY)
            ? price.add(limit.multiply(atr))
            : price.subtract(limit.multiply(atr));

        final BigDecimal scaled = raw.setScale(3, RoundingMode.DOWN);

        return scaled.doubleValue();
    }

    private BigDecimal calculateAtr() {
        final int atrPeriod = tradeConfigParameterService.getAtrPeriod();
        final double[] trValues = candleService.getTR(atrPeriod);
        double atr = 0.0;
        for (double tr : trValues) {
            atr += tr;
        }
        atr /= atrPeriod;
        return BigDecimal.valueOf(atr);
    }
}
