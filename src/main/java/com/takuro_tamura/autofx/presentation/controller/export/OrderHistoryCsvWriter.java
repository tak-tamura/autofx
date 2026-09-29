package com.takuro_tamura.autofx.presentation.controller.export;

import com.takuro_tamura.autofx.presentation.controller.response.OrderHistoryExportResponse;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistorySearchResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrderHistoryCsvWriter {
    private static final String HEADER =
        "orderId,currencyPair,side,size,status,fillDatetime,fillPrice,closeDatetime,closePrice,profit\r\n";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public byte[] write(OrderHistoryExportResponse response) {
        final StringBuilder csv = new StringBuilder(HEADER);
        response.orders().forEach(order -> csv.append(toRow(order)));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toRow(OrderHistorySearchResponse.Order order) {
        return String.join(",",
            value(order.orderId()),
            value(order.currencyPair()),
            value(order.side()),
            value(order.size()),
            value(order.status()),
            value(order.fillDatetime()),
            value(order.fillPrice()),
            value(order.closeDatetime()),
            value(order.closePrice()),
            value(order.profit())
        ) + "\r\n";
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }
        final String text = value instanceof LocalDateTime dateTime
            ? DATE_TIME_FORMATTER.format(dateTime)
            : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
