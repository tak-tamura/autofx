package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.application.CandleApplicationService;
import com.takuro_tamura.autofx.application.command.CandleImportCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CandleController {
    private final CandleApplicationService candleApplicationService;

    @PostMapping("/candle/import")
    public ResponseEntity<String> importCandles(@RequestBody CandleImportCommand command) {
        int numImported = candleApplicationService.importCandlesFromKlines(command);
        return ResponseEntity.ok(numImported + " candles imported.");
    }
}
