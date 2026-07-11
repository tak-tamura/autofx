package com.takuro_tamura.autofx.presentation.controller.response.factory;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.presentation.controller.response.OrderRecord;
import org.springframework.stereotype.Component;

@Component
public class OrderRecordFactory {
    
    public OrderRecord createOrderRecord(Order order, TimeFrame timeFrame) {
        return new OrderRecord(
            order.getOrderId(),
            order.getCurrencyPair(),
            order.getSide(),
            order.getSize(),
            order.getStatus(),
            order.getFillDatetime() != null ? 
                timeFrame.truncateTime(order.getFillDatetime()) : null,
            order.getFillPrice() != null ? 
                order.getFillPrice().getValue().doubleValue() : null,
            order.getCloseDatetime() != null ? 
                timeFrame.truncateTime(order.getCloseDatetime()) : null,
            order.getClosePrice() != null ? 
                order.getClosePrice().getValue().doubleValue() : null
        );
    }
}
