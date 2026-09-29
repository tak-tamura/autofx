package com.takuro_tamura.autofx.presentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takuro_tamura.autofx.application.OrderHistorySearchApplicationService;
import com.takuro_tamura.autofx.application.command.OrderHistorySearchCommand;
import com.takuro_tamura.autofx.presentation.controller.export.OrderHistoryCsvWriter;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistoryExportResponse;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistorySearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderHistorySearchApplicationService orderHistorySearchApplicationService;
    private final OrderHistoryCsvWriter orderHistoryCsvWriter;
    private final ObjectMapper objectMapper;

    @GetMapping("/history")
    public ResponseEntity<OrderHistorySearchResponse> searchOrderHistory(
        @PageableDefault Pageable page,
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(orderHistorySearchApplicationService.searchOrderHistory(
            new OrderHistorySearchCommand(
                page.getPageNumber(),
                page.getPageSize(),
                startDate,
                endDate
            )
        ));
    }

    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportOrderHistory(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        @RequestParam String format
    ) throws JsonProcessingException {
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "endDate must be on or after startDate");
        }

        final OrderHistoryExportResponse response =
            orderHistorySearchApplicationService.exportOrderHistory(startDate, endDate);
        final String normalizedFormat = format.toLowerCase(Locale.ROOT);
        final byte[] body;
        final MediaType contentType;

        switch (normalizedFormat) {
            case "csv" -> {
                body = orderHistoryCsvWriter.write(response);
                contentType = new MediaType("text", "csv", StandardCharsets.UTF_8);
            }
            case "json" -> {
                body = objectMapper.writeValueAsBytes(response);
                contentType = MediaType.APPLICATION_JSON;
            }
            default -> throw new ResponseStatusException(BAD_REQUEST, "format must be csv or json");
        }

        final String filename = "order-history_" + startDate + "_to_" + endDate + "." + normalizedFormat;
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(body);
    }
}
