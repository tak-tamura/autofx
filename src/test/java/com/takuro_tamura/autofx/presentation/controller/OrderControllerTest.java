package com.takuro_tamura.autofx.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.takuro_tamura.autofx.application.OrderHistorySearchApplicationService;
import com.takuro_tamura.autofx.presentation.controller.export.OrderHistoryCsvWriter;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistoryExportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {
    private OrderHistorySearchApplicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(OrderHistorySearchApplicationService.class);
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var controller = new OrderController(service, new OrderHistoryCsvWriter(), objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new RestExceptionHandler())
            .build();
    }

    @Test
    void downloadsJsonWithDateRangeMetadataAndNoStoreHeader() throws Exception {
        var response = emptyResponse();
        when(service.exportOrderHistory(response.startDate(), response.endDate())).thenReturn(response);

        mockMvc.perform(get("/api/v1/order/history/export")
                .param("startDate", "2026-09-01")
                .param("endDate", "2026-09-30")
                .param("format", "json"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"order-history_2026-09-01_to_2026-09-30.json\""))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.startDate").value("2026-09-01"))
            .andExpect(jsonPath("$.endDate").value("2026-09-30"))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.orders").isArray());
    }

    @Test
    void downloadsCsvWithUtf8ContentType() throws Exception {
        var response = emptyResponse();
        when(service.exportOrderHistory(response.startDate(), response.endDate())).thenReturn(response);

        mockMvc.perform(get("/api/v1/order/history/export")
                .param("startDate", "2026-09-01")
                .param("endDate", "2026-09-30")
                .param("format", "csv"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/csv;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"order-history_2026-09-01_to_2026-09-30.csv\""))
            .andExpect(content().string(
                "orderId,currencyPair,side,size,status,fillDatetime,fillPrice,closeDatetime,closePrice,profit\r\n"
            ));
    }

    @Test
    void rejectsUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/v1/order/history/export")
                .param("startDate", "2026-09-01")
                .param("endDate", "2026-09-30")
                .param("format", "xml"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/order/history/export")
                .param("startDate", "2026-09-30")
                .param("endDate", "2026-09-01")
                .param("format", "csv"))
            .andExpect(status().isBadRequest());
    }

    private OrderHistoryExportResponse emptyResponse() {
        return new OrderHistoryExportResponse(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            0,
            0,
            List.of()
        );
    }
}
