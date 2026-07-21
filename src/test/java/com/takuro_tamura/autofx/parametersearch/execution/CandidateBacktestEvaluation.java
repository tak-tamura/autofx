package com.takuro_tamura.autofx.parametersearch.execution;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

/** 1候補のパラメータ、取引台帳、および集計指標を対応付けた探索結果。 */
public record CandidateBacktestEvaluation(
    StrategyParameterSet parameters,
    BacktestResult backtestResult,
    BacktestMetrics metrics
) {
    public CandidateBacktestEvaluation {
        if (parameters == null || backtestResult == null || metrics == null) {
            throw new IllegalArgumentException("Candidate parameters, backtest result, and metrics are required");
        }
    }
}
