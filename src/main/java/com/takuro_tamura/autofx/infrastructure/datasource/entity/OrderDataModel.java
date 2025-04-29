package com.takuro_tamura.autofx.infrastructure.datasource.entity;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDataModel {
    private Long orderId;
    private CurrencyPair currencyPair;
    private OrderSide side;
    private Integer size;
    private OrderStatus status;
    private LocalDateTime createdDatetime;
    private LocalDateTime fillDatetime;
    private BigDecimal fillPrice;
    private LocalDateTime closeDatetime;
    private BigDecimal closePrice;

    public OrderDataModel() {}

    public OrderDataModel(Order order) {
        this.orderId = order.getOrderId();
        this.currencyPair = order.getCurrencyPair();
        this.side = order.getSide();
        this.size = order.getSize();
        this.status = order.getStatus();
        this.fillDatetime = order.getFillDatetime();
        this.fillPrice = order.getFillPrice() != null ? order.getFillPrice().getValue() : null;
        this.closeDatetime = order.getCloseDatetime();
        this.closePrice = order.getClosePrice() != null ? order.getClosePrice().getValue() : null;
    }

    public Order toModel() {
        return new Order(
            this.orderId,
            this.currencyPair,
            this.side,
            this.size,
            this.status,
            this.fillDatetime,
            this.fillPrice != null ? new Price(this.fillPrice) : null,
            this.closeDatetime,
            this.closePrice != null ? new Price(this.closePrice) : null
        );
    }
}
