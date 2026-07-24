package com.takuro_tamura.autofx.parametersearch.paper;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestRiskParameters;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

import java.time.Instant;
import java.util.List;

/**
 * paper候補と現在値の差分を固定した手動レビュー用計画。
 * applyAllowedとliveTradingAllowedは常にfalseであり、自動適用命令を表現しない。
 */
public record PaperCandidatePreparationPlan(
    int schemaVersion,
    Instant generatedAt,
    String datasetId,
    String datasetSha256,
    int candidateRank,
    boolean manualReviewRequired,
    boolean applyAllowed,
    boolean liveTradingAllowed,
    PaperCurrentConfiguration currentConfiguration,
    StrategyParameterSet proposedStrategyParameters,
    int proposedAtrPeriod,
    BacktestAssumptions executionAssumptions,
    BacktestRiskParameters backtestRiskParameters,
    List<ParameterDifference> differences
) {
    /** 設定名ごとの現在値と候補値。変更がない項目もレビューのため残す。 */
    public record ParameterDifference(String parameter, String currentValue, String proposedValue, boolean changed) {
    }
}
