package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.OrderHistorySearchCommand;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistorySearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderHistorySearchApplicationService {
    private final OrderService orderService;

    private final OrderRepository orderRepository;

    public OrderHistorySearchResponse searchOrderHistory(OrderHistorySearchCommand command) {
        final List<Order> foundOrders = orderRepository.findByDateRange(
            command.startDate().atStartOfDay(),
            command.endDate().atTime(23, 59, 59, 999999)
        );

        final BigDecimal profit = orderService.accumulateProfit(foundOrders);

        final List<OrderHistorySearchResponse.Order> orders = foundOrders.stream()
            .skip((long) command.size() * command.page())
            .limit(command.size())
            .map(order -> new OrderHistorySearchResponse.Order(
                order.getOrderId(),
                order.getCurrencyPair(),
                order.getSide(),
                order.getSize(),
                order.getStatus(),
                order.getFillDatetime(),
                order.getFillPrice().getValue().doubleValue(),
                order.getCloseDatetime(),
                order.getClosePrice() != null ? order.getClosePrice().getValue().doubleValue() : null,
                order.calculateProfit().doubleValue()
            ))
            .toList();

        return new OrderHistorySearchResponse(
            foundOrders.size(),
            (int) Math.ceil((double) foundOrders.size() / command.size()),
            orders,
            profit.doubleValue()
        );
    }
}
