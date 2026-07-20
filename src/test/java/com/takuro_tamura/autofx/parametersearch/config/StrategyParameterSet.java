package com.takuro_tamura.autofx.parametersearch.config;

import com.takuro_tamura.autofx.domain.strategy.config.StrategyConfig;

import java.math.BigDecimal;

/**
 * 1回のバックテストへ渡すインジケーターパラメータ一式。
 * 小数は探索中の同一性を安定して判定できるようBigDecimalで保持する。
 */
public record StrategyParameterSet(
    int emaShortPeriod,
    int emaLongPeriod,
    int rsiPeriod,
    int macdFastPeriod,
    int macdSlowPeriod,
    int macdSignalPeriod,
    int bBandsPeriod,
    BigDecimal bBandsMultiplier,
    int adxPeriod,
    BigDecimal adxThreshold
) {
    public StrategyParameterSet {
        if (emaShortPeriod <= 0 || emaLongPeriod <= 0 || rsiPeriod <= 0
            || macdFastPeriod <= 0 || macdSlowPeriod <= 0 || macdSignalPeriod <= 0
            || bBandsPeriod <= 0 || adxPeriod <= 0
            || bBandsMultiplier == null || bBandsMultiplier.signum() <= 0
            || adxThreshold == null || adxThreshold.signum() <= 0) {
            throw new IllegalArgumentException("All strategy parameters must be greater than zero");
        }
        if (emaShortPeriod >= emaLongPeriod) {
            throw new IllegalArgumentException("EMA short period must be less than EMA long period");
        }
        if (macdFastPeriod >= macdSlowPeriod) {
            throw new IllegalArgumentException("MACD fast period must be less than MACD slow period");
        }
    }

    /** 本番と共通の戦略実装へ探索候補を渡すための設定に変換する。 */
    public StrategyConfig toStrategyConfig() {
        return new StrategyConfig(
            emaShortPeriod,
            emaLongPeriod,
            rsiPeriod,
            macdFastPeriod,
            macdSlowPeriod,
            macdSignalPeriod,
            bBandsPeriod,
            bBandsMultiplier.doubleValue(),
            adxPeriod,
            adxThreshold.doubleValue()
        );
    }
}
