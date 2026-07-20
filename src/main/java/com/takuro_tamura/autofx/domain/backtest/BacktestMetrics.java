package com.takuro_tamura.autofx.domain.backtest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 1回のバックテスト結果から算出した、パラメータ候補比較用の標準評価指標。
 * grossLossとaverageLossは比較しやすいよう損失額の絶対値で保持する。
 */
public record BacktestMetrics(
    int tradeCount,
    int winningTradeCount,
    int losingTradeCount,
    int breakEvenTradeCount,
    BigDecimal winRate,
    BigDecimal grossProfit,
    BigDecimal grossLoss,
    BigDecimal netProfit,
    BigDecimal averageWin,
    BigDecimal averageLoss,
    Optional<BigDecimal> profitFactor,
    BigDecimal maximumDrawdown,
    int maximumConsecutiveWins,
    int maximumConsecutiveLosses,
    List<BigDecimal> rMultiples,
    Optional<BigDecimal> averageR,
    BigDecimal exposure,
    BigDecimal totalTransactionCosts
) {
    public BacktestMetrics {
        profitFactor = profitFactor == null ? Optional.empty() : profitFactor;
        averageR = averageR == null ? Optional.empty() : averageR;
        rMultiples = List.copyOf(rMultiples);
    }
}
