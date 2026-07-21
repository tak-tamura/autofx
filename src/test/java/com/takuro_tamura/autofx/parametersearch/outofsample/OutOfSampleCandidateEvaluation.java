package com.takuro_tamura.autofx.parametersearch.outofsample;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;

/**
 * Phase 6で固定した候補と、そのOut-of-sampleバックテスト結果を対応付ける。
 * In-sample順位も保持し、Out-of-sample成績による並べ替えを防ぐ。
 */
public record OutOfSampleCandidateEvaluation(
    RankedCandidate selectedCandidate,
    BacktestResult backtestResult,
    BacktestMetrics metrics
) {
    public OutOfSampleCandidateEvaluation {
        if (selectedCandidate == null || !selectedCandidate.selected()
            || backtestResult == null || metrics == null) {
            throw new IllegalArgumentException("Selected candidate, backtest result, and metrics are required");
        }
    }
}
