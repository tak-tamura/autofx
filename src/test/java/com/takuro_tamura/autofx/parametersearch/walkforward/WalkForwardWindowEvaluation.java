package com.takuro_tamura.autofx.parametersearch.walkforward;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;

/** 1候補を1つのウォークフォワード区間で評価した取引台帳と指標。 */
public record WalkForwardWindowEvaluation(
    WalkForwardWindow window,
    BacktestResult backtestResult,
    BacktestMetrics metrics
) {
    public WalkForwardWindowEvaluation {
        if (window == null || backtestResult == null || metrics == null) {
            throw new IllegalArgumentException("Window, backtest result, and metrics are required");
        }
    }
}
