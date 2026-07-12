package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.application.BacktestChartApplicationService;
import com.takuro_tamura.autofx.application.ChartApplicationService;
import com.takuro_tamura.autofx.application.command.chart.GetChartCommand;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.presentation.controller.response.ChartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * チャート表示用API
 * TRADE.BACK_TEST 設定に応じて、ライブチャート or バックテストチャートを返す
 */
@RestController
@RequiredArgsConstructor
public class ChartController {

    private final ChartApplicationService chartApplicationService;
    private final BacktestChartApplicationService backtestChartApplicationService;
    private final TradeConfigParameterService tradeConfigParameterService;

    /**
     * チャートデータ取得
     * TRADE.BACK_TEST がtrueの場合はバックテスト結果を返す
     * falseの場合はライブトレード用のチャートを返す
     */
    @PostMapping("/api/chart")
    @ResponseBody
    public ChartResponse getChart(@RequestBody GetChartCommand command) {
        if (tradeConfigParameterService.isBackTest()) {
            return backtestChartApplicationService.getBacktestChart(command);
        } else {
            return chartApplicationService.getChart(command);
        }
    }
}
