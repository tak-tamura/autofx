package com.takuro_tamura.autofx.presentation.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderRecord(
    Long orderId,
    CurrencyPair currencyPair,
    OrderSide orderSide,
    int quantity,
    OrderStatus status,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Asia/Tokyo")
    LocalDateTime fillDatetime,
    Double fillPrice,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Asia/Tokyo")
    LocalDateTime closeDatetime,
    Double closePrice
) {
}
