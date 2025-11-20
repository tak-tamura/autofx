package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.application.OrderHistorySearchApplicationService;
import com.takuro_tamura.autofx.application.command.OrderHistorySearchCommand;
import com.takuro_tamura.autofx.presentation.controller.response.OrderHistorySearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderHistorySearchApplicationService orderHistorySearchApplicationService;

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
}
