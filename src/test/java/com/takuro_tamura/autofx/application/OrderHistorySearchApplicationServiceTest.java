package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderHistorySearchApplicationServiceTest {
    private final OrderService orderService = mock(OrderService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderHistorySearchApplicationService service =
        new OrderHistorySearchApplicationService(orderService, orderRepository);

    @Test
    void exportsEveryOrderInTheInclusiveDateRangeWithoutPagination() {
        var startDate = LocalDate.of(2026, 9, 1);
        var endDate = LocalDate.of(2026, 9, 30);
        var firstOrder = order(1L, LocalDateTime.of(2026, 9, 1, 0, 0));
        var lastOrder = order(2L, LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        var orders = List.of(firstOrder, lastOrder);
        when(orderRepository.findByDateRange(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59, 999999)
        )).thenReturn(orders);
        when(orderService.accumulateProfit(orders)).thenReturn(new BigDecimal("250.5"));

        var response = service.exportOrderHistory(startDate, endDate);

        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.profit()).isEqualTo(250.5);
        assertThat(response.orders()).extracting(order -> order.orderId())
            .containsExactly(1L, 2L);
        verify(orderService).accumulateProfit(orders);
    }

    private Order order(long orderId, LocalDateTime fillDatetime) {
        return new Order(
            orderId,
            CurrencyPair.USD_JPY,
            OrderSide.BUY,
            1_000,
            OrderStatus.FILLED,
            fillDatetime,
            new Price(new BigDecimal("150.125")),
            null,
            null
        );
    }
}
