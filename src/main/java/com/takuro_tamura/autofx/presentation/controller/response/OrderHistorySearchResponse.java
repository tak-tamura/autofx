package com.takuro_tamura.autofx.presentation.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public record OrderHistorySearchResponse(
    int totalElements,
    int totalPages,
    List<Order> orders,
    double profit
) {
    public record Order(
        long orderId,
        CurrencyPair currencyPair,
        OrderSide side,
        double size,
        OrderStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fillDatetime,
        Double fillPrice,
        @Nullable
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime closeDatetime,
        @Nullable Double closePrice
    ) {}
}