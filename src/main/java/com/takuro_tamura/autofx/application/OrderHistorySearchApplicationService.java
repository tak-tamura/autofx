package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.OrderHistorySearchCommand;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistoryExportResponse;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistorySearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderHistorySearchApplicationService {
    private final OrderService orderService;

    private final OrderRepository orderRepository;

    public OrderHistorySearchResponse searchOrderHistory(OrderHistorySearchCommand command) {
        final List<Order> foundOrders = findOrders(command.startDate(), command.endDate());

        final BigDecimal profit = orderService.accumulateProfit(foundOrders);

        final List<OrderHistorySearchResponse.Order> orders = foundOrders.stream()
            .skip((long) command.size() * command.page())
            .limit(command.size())
            .map(this::toResponseOrder)
            .toList();

        return new OrderHistorySearchResponse(
            foundOrders.size(),
            (int) Math.ceil((double) foundOrders.size() / command.size()),
            orders,
            profit.doubleValue()
        );
    }

    public OrderHistoryExportResponse exportOrderHistory(
        LocalDate startDate,
        LocalDate endDate
    ) {
        final List<Order> foundOrders = findOrders(startDate, endDate);
        final List<OrderHistorySearchResponse.Order> orders = foundOrders.stream()
            .map(this::toResponseOrder)
            .toList();

        return new OrderHistoryExportResponse(
            startDate,
            endDate,
            foundOrders.size(),
            orderService.accumulateProfit(foundOrders).doubleValue(),
            orders
        );
    }

    private List<Order> findOrders(LocalDate startDate, LocalDate endDate) {
        return orderRepository.findByDateRange(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59, 999999)
        );
    }

    private OrderHistorySearchResponse.Order toResponseOrder(Order order) {
        return new OrderHistorySearchResponse.Order(
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
        );
    }
}
