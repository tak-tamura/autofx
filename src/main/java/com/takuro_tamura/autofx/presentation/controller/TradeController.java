package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.application.TradeSettingApplicationService;
import com.takuro_tamura.autofx.presentation.controller.request.ScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
public class TradeController {
    private final TradeSettingApplicationService tradeSettingApplicationService;

    @PostMapping("/suspend")
    public ResponseEntity<?> suspendTrade() {
        tradeSettingApplicationService.suspendTrade();
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/suspend/schedule")
    public ResponseEntity<?> scheduleSuspendTrade(@RequestBody ScheduleRequest request) {
        tradeSettingApplicationService.scheduleSuspendTrade(request.getTime());
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/resume")
    public ResponseEntity<?> resumeTrade() {
        tradeSettingApplicationService.resumeTrade();
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/resume/schedule")
    public ResponseEntity<?> scheduleResumeTrade(@RequestBody ScheduleRequest request) {
        tradeSettingApplicationService.scheduleResumeTrade(request.getTime());
        return ResponseEntity.ok("ok");
    }
}
