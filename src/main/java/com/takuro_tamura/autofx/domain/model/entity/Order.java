package com.takuro_tamura.autofx.domain.model.entity;

import com.takuro_tamura.autofx.domain.model.value.*;
import com.takuro_tamura.autofx.presentation.controller.response.OrderRecord;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@ToString
@AllArgsConstructor
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
    }

    public void close(LocalDateTime time, Price closePrice) {
        this.closeDatetime = time;
        this.closePrice = closePrice;
        this.status = OrderStatus.CLOSED;
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

    public OrderRecord toRecord(TimeFrame timeFrame) {
        return new OrderRecord(
            orderId,
            currencyPair,
            side,
            size,
            status,
            fillDatetime != null ? timeFrame.truncateTime(fillDatetime) : null,
            fillPrice != null ? fillPrice.getValue().doubleValue() : null,
            closeDatetime != null ? timeFrame.truncateTime(closeDatetime) : null,
            closePrice != null ? closePrice.getValue().doubleValue() : null
        );
    }
}
