package com.takuro_tamura.autofx.parametersearch.paper;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestRiskParameters;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.config.WalkForwardCriteria;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetMetadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Phase 9で固定したpaper候補manifestの読み取り専用モデル。
 * Phase 10では内容の検証とレビュー資料の生成にだけ使用し、設定更新には使用しない。
 */
public record PaperCandidateManifest(
    int schemaVersion,
    Instant generatedAt,
    boolean manualReviewRequired,
    boolean liveTradingAllowed,
    String strategyClass,
    HistoricalDatasetMetadata dataset,
    BacktestAssumptions executionAssumptions,
    BacktestRiskParameters riskParameters,
    WalkForwardCriteria walkForwardCriteria,
    List<PaperCandidate> candidates
) {
    /** paper運用へ進める候補と、その選定根拠となる評価値。 */
    public record PaperCandidate(
        int inSampleRank,
        StrategyParameterSet parameters,
        MetricSnapshot inSampleMetrics,
        MetricSnapshot outOfSampleMetrics,
        BigDecimal profitableWindowRate,
        BigDecimal positiveAverageRWindowRate
    ) {
    }

    /** manifestで保存する再現性確認用のバックテスト指標。 */
    public record MetricSnapshot(
        int tradeCount,
        BigDecimal winRate,
        BigDecimal netProfit,
        BigDecimal profitFactor,
        BigDecimal maximumDrawdown,
        int maximumConsecutiveLosses,
        BigDecimal averageR,
        BigDecimal exposure,
        BigDecimal transactionCosts
    ) {
    }
}
