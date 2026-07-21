package com.takuro_tamura.autofx.domain.backtest;

import java.math.BigDecimal;

/**
 * バックテストで固定するATR期間と損切り・利益確定倍率。
 * DBの稼働設定から分離し、同じ探索条件を再現できるようにする。
 */
public record BacktestRiskParameters(
    int atrPeriod,
    BigDecimal stopMultiplier,
    BigDecimal profitMultiplier
) {
    public BacktestRiskParameters {
        if (atrPeriod <= 0) {
            throw new IllegalArgumentException("ATR period must be greater than zero");
        }
        if (stopMultiplier == null || stopMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Stop multiplier must be greater than zero");
        }
        if (profitMultiplier == null || profitMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Profit multiplier must be greater than zero");
        }
    }
}
