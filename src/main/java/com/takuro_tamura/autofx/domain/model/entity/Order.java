package com.takuro_tamura.autofx.domain.model.entity;

import com.takuro_tamura.autofx.domain.model.value.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@ToString
public class Order {
    @Setter
    private Long orderId;
    private final CurrencyPair currencyPair;
    private final OrderSide side;
    private final int size;
    @Setter
    private OrderStatus status;
    private LocalDateTime fillDatetime;
    private Price fillPrice;
    @Setter
    private LocalDateTime closeDatetime;
    @Setter
    private Price closePrice;
    private ProtectionLevels protectionLevels;

    public Order(
        Long orderId,
        CurrencyPair currencyPair,
        OrderSide side,
        int size,
        OrderStatus status,
        LocalDateTime fillDatetime,
        Price fillPrice,
        LocalDateTime closeDatetime,
        Price closePrice
    ) {
        this.orderId = orderId;
        this.currencyPair = currencyPair;
        this.side = side;
        this.size = size;
        this.status = status;
        this.fillDatetime = fillDatetime;
        this.fillPrice = fillPrice;
        this.closeDatetime = closeDatetime;
        this.closePrice = closePrice;
        this.protectionLevels = null;
    }

    @Builder(builderMethodName = "newOrder")
    public Order(
        Long orderId,
        CurrencyPair currencyPair,
        OrderSide side,
        int size,
        LocalDateTime fillDatetime,
        Price price
    ) {
        this.orderId = orderId;
        this.currencyPair = currencyPair;
        this.side = side;
        this.size = size;
        this.status = OrderStatus.FILLED;
        this.fillDatetime = fillDatetime;
        this.fillPrice = price;
        this.protectionLevels = null;
    }

    public void close(LocalDateTime time, Price closePrice) {
        this.closeDatetime = time;
        this.closePrice = closePrice;
        this.status = OrderStatus.CLOSED;
    }

    public void fixProtectionLevels(ProtectionLevels protectionLevels) {
        if (protectionLevels == null) {
            throw new IllegalArgumentException("Protection levels are required");
        }
        if (this.protectionLevels != null) {
            throw new IllegalStateException("Protection levels are already fixed");
        }
        this.protectionLevels = protectionLevels;
    }

    public BigDecimal calculateProfit() {
        if (status != OrderStatus.CLOSED) {
            return BigDecimal.ZERO;
        }

        final BigDecimal diff = switch (side) {
            case BUY -> closePrice.subtract(fillPrice).getValue();
            case SELL -> fillPrice.subtract(closePrice).getValue();
        };
        return diff.multiply(BigDecimal.valueOf(size));
    }
}
