package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.application.ChartApplicationService;
import com.takuro_tamura.autofx.application.command.chart.GetChartCommand;
import com.takuro_tamura.autofx.presentation.controller.response.ChartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChartController {

    private final ChartApplicationService chartApplicationService;

    @PostMapping("/api/chart")
    @ResponseBody
    public ChartResponse getChart(@RequestBody GetChartCommand command) {
        return chartApplicationService.getChart(command);
    }
}
