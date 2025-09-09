package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.external.enums.ExecutionType;
import com.takuro_tamura.autofx.infrastructure.external.request.CloseOrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.request.OrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.response.OrderResponse;
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
    private final OrderRepository orderRepository;
    private final PrivateApi privateApi;
    private final CandleService candleService;
    private final RedisCacheService redisCacheService;
    private final TradeConfigParameterService tradeConfigParameterService;

    public boolean shouldCloseOrder(Order order, Price currentPrice) {
        if (order == null || order.getStatus() != OrderStatus.FILLED) {
            return false;
        }

        // シグナルがオーダータイプと反していればcloseする
//        if (signal != order.getSide()) {
//            return true;
//        }

        // 利益確定・損切りラインに達していたらclose
        final BigDecimal profitLimit = tradeConfigParameterService.getProfitLimit();
        final BigDecimal stopLimit = tradeConfigParameterService.getStopLimit();

        if (order.getSide() == OrderSide.BUY) {
            // 利益確定
            if (currentPrice.subtract(order.getFillPrice()).getValue().compareTo(profitLimit) >= 0) {
                return true;
            }
            // 損切り
            return order.getFillPrice().subtract(currentPrice).getValue().compareTo(stopLimit) >= 0;
        } else {
            // 利益確定
            if (order.getFillPrice().subtract(currentPrice).getValue().compareTo(profitLimit) >= 0) {
                return true;
            }
            // 損切り
            return currentPrice.subtract(order.getFillPrice()).getValue().compareTo(stopLimit) >= 0;
        }
    }

    public boolean canOrder(Order lastOrder) {
        return lastOrder == null || lastOrder.getStatus().isCompleted();
    }

    /**
     * 新規オーダー
     * @param currencyPair
     * @param type
     * @param size
     */
    public void makeOrder(CurrencyPair currencyPair, OrderSide type, int size) {
        final OrderRequest request = OrderRequest.newMarketOrder()
            .currencyPair(currencyPair)
            .side(type)
            .size(size)
            .build();
        log.info("Sending new order request: {}", request);

        final OrderResponse response = privateApi.order(request);
        log.info("Sent order request, order id: {}", response.getOrderId());
    }

    /**
     * 決済オーダー
     * @param order
     */
    public void closeOrder(Order order) {
        final CloseOrderRequest request = CloseOrderRequest.builder()
            .currencyPair(order.getCurrencyPair())
            .executionType(ExecutionType.MARKET)
            .side(OrderSide.getCloseSide(order.getSide()))
            .size(order.getSize())
            .build();
        log.info("Sending close order request: {}", request);

        final OrderResponse response = privateApi.closeOrder(request).get(0);
        log.info("Sent close order request, orderId: {}", response.getOrderId());

        // 約定時にオーダー情報を更新するため、決済注文のオーダーIDをキーにして決済対象のオーダーのオーダーIDをRedisに保存しておく
        redisCacheService.save(CacheKey.CLOSE_ORDER_ID.build(response.getOrderId().toString()), order.getOrderId());
        log.info("Store orderId({}) with key({})", order.getOrderId(), response.getOrderId());
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


        final CloseOrderRequest request = CloseOrderRequest.builder()
            .currencyPair(order.getCurrencyPair())
            .executionType(ExecutionType.OCO)
            .side(OrderSide.getCloseSide(order.getSide()))
            .size(order.getSize())
            .stopPrice(stopPrice)
            .limitPrice(limitPrice)
            .build();


        final List<OrderResponse> response = privateApi.closeOrder(request);
        log.info("Sent OCO order request, orderId: {}", order.getOrderId());

        // 約定時にオーダー情報を更新するため、決済注文のオーダーIDをキーにして決済対象のオーダーのオーダーIDをRedisに保存しておく
        response.forEach(it -> {
            redisCacheService.save(CacheKey.CLOSE_ORDER_ID.build(it.getOrderId().toString()), order.getOrderId());
            log.info("Store orderId({}) with key({})", order.getOrderId(), it.getOrderId());
        });
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
        if (shouldCloseOrder(lastOrder, candle.getClose())) {
            lastOrder.close(candle.getTime(), candle.getClose());
        } else {
            if (canOrder(lastOrder)) {
                final Order order = createDummyOrder(candle, side, candle.getClose());
                orders.add(order);
            } else {
                if (lastOrder != null && lastOrder.getSide() != side) {
                    lastOrder.close(candle.getTime(), candle.getClose());
                }
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
        if (side == OrderSide.BUY) {
            return price.subtract(limit.multiply(atr)).setScale(3, RoundingMode.DOWN).doubleValue();
        } else {
            return price.add(limit.multiply(atr)).doubleValue();
        }
    }

    private double calculateLimitPrice(BigDecimal price, BigDecimal limit, BigDecimal atr, OrderSide side) {
        if (side == OrderSide.BUY) {
            return price.add(limit.multiply(atr)).setScale(3, RoundingMode.DOWN).doubleValue();
        } else {
            return price.subtract(limit.multiply(atr)).doubleValue();
        }
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
