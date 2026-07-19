package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.ProtectionLevels;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.indicator.AtrCalculator;
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

        final ProtectionLevels protectionLevels = order.getProtectionLevels() != null
            ? order.getProtectionLevels()
            : createProtectionLevels(
                order,
                calculateLatestAtr(),
                tradeConfigParameterService.getStopLimit(),
                tradeConfigParameterService.getProfitLimit()
            );

        final BigDecimal stopPrice = protectionLevels.stopPrice().getValue();
        final BigDecimal limitPrice = protectionLevels.takeProfitPrice().getValue();

        if (order.getSide() == OrderSide.BUY) {
            // 利益確定
            if (currentPrice.getValue().compareTo(limitPrice) >= 0) {
                return true;
            }
            // 損切り
            return currentPrice.getValue().compareTo(stopPrice) <= 0;
        } else {
            // 利益確定
            if (currentPrice.getValue().compareTo(limitPrice) <= 0) {
                return true;
            }
            // 損切り
            return currentPrice.getValue().compareTo(stopPrice) >= 0;
        }
    }

    public boolean hasOpenPosition(Order lastOrder) {
        return lastOrder != null && !lastOrder.getStatus().isCompleted();
    }

    /**
     * 新規オーダー
     * @param currencyPair
     * @param type
     * @param size
     */
    public Long makeOrder(CurrencyPair currencyPair, OrderSide type, int size) {
        log.info("Sending new order request: currencyPair={}, side={}, size={}", currencyPair, type, size);
        final Long orderId = orderPlacementPort.submitMarketOrder(currencyPair, type, size);
        log.info("Sent order request successfully");
        return orderId;
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
        makeStopAndProfitOrder(order, calculateLatestAtr());
    }

    public void makeStopAndProfitOrder(Order order, BigDecimal atr) {
        log.info("Calculated ATR: {}", atr.doubleValue());

        final ProtectionLevels protectionLevels = createProtectionLevels(
            order,
            atr,
            tradeConfigParameterService.getStopLimit(),
            tradeConfigParameterService.getProfitLimit()
        );
        final double stopPrice = protectionLevels.stopPrice().getValue().doubleValue();
        log.info("Calculated stopPrice: {}", stopPrice);

        final double limitPrice = protectionLevels.takeProfitPrice().getValue().doubleValue();
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

    public Order handleBackTestOrder(OrderSide side, List<Order> orders, Order lastOrder, Candle candle) {
        if (hasOpenPosition(lastOrder)) {
            closeBackTestOrderIfOpposite(side, lastOrder, candle);
            return null;
        }
        final Order order = createBackTestOrder(side, candle);
        orders.add(order);
        return order;
    }

    public Order createBackTestOrder(OrderSide side, Candle candle) {
        return createBackTestOrder(side, candle, candle.getClose());
    }

    public Order createBackTestOrder(OrderSide side, Candle candle, Price fillPrice) {
        final Order order = createDummyOrder(candle, side, fillPrice);
        log.debug("Create new order at {}, side: {}, price: {}", candle.getTime(), side, fillPrice);
        return order;
    }

    void closeBackTestOrderIfOpposite(OrderSide signalSide, Order openOrder, Candle candle) {
        if (openOrder.getSide() != signalSide) {
            openOrder.close(candle.getTime(), candle.getClose());
            log.debug("Close order at {}, side: {}, price: {}, profit: {}",
                candle.getTime(),
                openOrder.getSide(),
                candle.getClose(),
                openOrder.calculateProfit()
            );
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

    public ProtectionLevels createProtectionLevels(
        Order order,
        BigDecimal atr,
        BigDecimal stopMultiplier,
        BigDecimal profitMultiplier
    ) {
        if (order == null || order.getFillPrice() == null || order.getSide() == null) {
            throw new IllegalArgumentException("Filled order is required to calculate protection levels");
        }
        if (atr == null || atr.signum() <= 0) {
            throw new IllegalArgumentException("ATR must be greater than zero");
        }
        if (stopMultiplier == null || stopMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Stop multiplier must be greater than zero");
        }
        if (profitMultiplier == null || profitMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Profit multiplier must be greater than zero");
        }

        final Price stopPrice = calculateStopPrice(
            order.getFillPrice().getValue(),
            stopMultiplier,
            atr,
            order.getSide()
        );
        final Price takeProfitPrice = calculateLimitPrice(
            order.getFillPrice().getValue(),
            profitMultiplier,
            atr,
            order.getSide()
        );
        return new ProtectionLevels(atr, stopPrice, takeProfitPrice);
    }

    private Price calculateStopPrice(BigDecimal price, BigDecimal limit, BigDecimal atr, OrderSide side) {
        final BigDecimal raw = (side == OrderSide.BUY)
            ? price.subtract(limit.multiply(atr))
            : price.add(limit.multiply(atr));

        final BigDecimal scaled = raw.setScale(3, RoundingMode.DOWN);

        return new Price(scaled);
    }

    private Price calculateLimitPrice(BigDecimal price, BigDecimal limit, BigDecimal atr, OrderSide side) {
        final BigDecimal raw = (side == OrderSide.BUY)
            ? price.add(limit.multiply(atr))
            : price.subtract(limit.multiply(atr));

        final BigDecimal scaled = raw.setScale(3, RoundingMode.DOWN);

        return new Price(scaled);
    }

    private BigDecimal calculateLatestAtr() {
        final int atrPeriod = tradeConfigParameterService.getAtrPeriod();
        final List<Candle> candles = candleService.getLatestCandles(atrPeriod + 1);
        return AtrCalculator.calculate(candles, candles.size() - 1, atrPeriod);
    }
}
