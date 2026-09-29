package com.takuro_tamura.autofx.presentation.controller.export;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistoryExportResponse;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistorySearchResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderHistoryCsvWriterTest {
    private final OrderHistoryCsvWriter writer = new OrderHistoryCsvWriter();

    @Test
    void writesHeaderAndAllOrderFieldsUsingUtf8AndCrLf() {
        var order = new OrderHistorySearchResponse.Order(
            123L,
            CurrencyPair.USD_JPY,
            OrderSide.BUY,
            1_000,
            OrderStatus.CLOSED,
            LocalDateTime.of(2026, 9, 1, 10, 20, 30),
            146.125,
            LocalDateTime.of(2026, 9, 2, 11, 21, 31),
            147.25,
            1125.0
        );
        var response = new OrderHistoryExportResponse(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            1,
            1125.0,
            List.of(order)
        );

        var csv = new String(writer.write(response), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo(
            "orderId,currencyPair,side,size,status,fillDatetime,fillPrice,closeDatetime,closePrice,profit\r\n" +
            "123,USD_JPY,BUY,1000.0,CLOSED,2026-09-01T10:20:30,146.125,2026-09-02T11:21:31,147.25,1125.0\r\n"
        );
    }

    @Test
    void leavesNullableCloseFieldsEmpty() {
        var order = new OrderHistorySearchResponse.Order(
            124L,
            CurrencyPair.EUR_JPY,
            OrderSide.SELL,
            500,
            OrderStatus.FILLED,
            LocalDateTime.of(2026, 9, 3, 1, 2, 3),
            170.5,
            null,
            null,
            0
        );
        var response = new OrderHistoryExportResponse(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            1,
            0,
            List.of(order)
        );

        var csv = new String(writer.write(response), StandardCharsets.UTF_8);

        assertThat(csv).contains(
            "124,EUR_JPY,SELL,500.0,FILLED,2026-09-03T01:02:03,170.5,,,0.0\r\n"
        );
    }
}
