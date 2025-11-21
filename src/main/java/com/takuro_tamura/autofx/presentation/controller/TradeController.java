package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.application.TradeConfigParameterApplicationService;
import com.takuro_tamura.autofx.application.TradeStateApplicationService;
import com.takuro_tamura.autofx.application.command.TradeConfigUpdateCommand;
import com.takuro_tamura.autofx.presentation.controller.request.ScheduleRequest;
import com.takuro_tamura.autofx.presentation.controller.response.TradeConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
public class TradeController {
    private final TradeStateApplicationService tradeSettingApplicationService;

    private final TradeConfigParameterApplicationService tradeConfigParameterApplicationService;

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

    @GetMapping("/config")
    public TradeConfigResponse getTradeConfig() {
        return tradeConfigParameterApplicationService.getTradeConfig();
    }

    @PostMapping("/config")
    public TradeConfigResponse updateTradeConfig(@RequestBody TradeConfigUpdateCommand command) {
        tradeConfigParameterApplicationService.updateTradeConfig(command);
        return tradeConfigParameterApplicationService.getTradeConfig();
    }
}
