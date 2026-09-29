package com.takuro_tamura.autofx.presentation.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record OrderHistoryExportResponse(
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,
    int totalElements,
    double profit,
    List<OrderHistorySearchResponse.Order> orders
) {}
