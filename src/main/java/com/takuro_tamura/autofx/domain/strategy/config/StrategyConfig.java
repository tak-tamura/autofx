package com.takuro_tamura.autofx.domain.strategy.config;

/**
 * Strategy実行に必要なすべてのパラメータを保持する値オブジェクト
 */
public record StrategyConfig(
    int emaPeriod1,
    int emaPeriod2,
    int rsiPeriod,
    int macdFastPeriod,
    int macdSlowPeriod,
    int macdSignalPeriod,
    int bBandsN,
    double bBandsK,
    int adxPeriod,
    double adxThreshold
) {}
