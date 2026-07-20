package com.takuro_tamura.autofx.parametersearch.config;

import java.math.BigDecimal;
import java.util.List;

/**
 * 探索の基準値、各パラメータの候補値、および実行量の安全上限を保持する。
 */
public record StrategySearchSpace(
    SearchMode mode,
    int maxCandidates,
    StrategyParameterSet baseline,
    List<Integer> emaShortPeriods,
    List<Integer> emaLongPeriods,
    List<Integer> rsiPeriods,
    List<Integer> macdFastPeriods,
    List<Integer> macdSlowPeriods,
    List<Integer> macdSignalPeriods,
    List<Integer> bBandsPeriods,
    List<BigDecimal> bBandsMultipliers,
    List<Integer> adxPeriods,
    List<BigDecimal> adxThresholds
) {
    public StrategySearchSpace {
        if (mode == null || baseline == null || maxCandidates <= 0) {
            throw new IllegalArgumentException("Search mode, baseline, and positive candidate limit are required");
        }
        emaShortPeriods = immutableNonEmpty(emaShortPeriods, "EMA short periods");
        emaLongPeriods = immutableNonEmpty(emaLongPeriods, "EMA long periods");
        rsiPeriods = immutableNonEmpty(rsiPeriods, "RSI periods");
        macdFastPeriods = immutableNonEmpty(macdFastPeriods, "MACD fast periods");
        macdSlowPeriods = immutableNonEmpty(macdSlowPeriods, "MACD slow periods");
        macdSignalPeriods = immutableNonEmpty(macdSignalPeriods, "MACD signal periods");
        bBandsPeriods = immutableNonEmpty(bBandsPeriods, "Bollinger Bands periods");
        bBandsMultipliers = immutableNonEmpty(bBandsMultipliers, "Bollinger Bands multipliers");
        adxPeriods = immutableNonEmpty(adxPeriods, "ADX periods");
        adxThresholds = immutableNonEmpty(adxThresholds, "ADX thresholds");

        requireBaseline(emaShortPeriods, baseline.emaShortPeriod(), "EMA short");
        requireBaseline(emaLongPeriods, baseline.emaLongPeriod(), "EMA long");
        requireBaseline(rsiPeriods, baseline.rsiPeriod(), "RSI");
        requireBaseline(macdFastPeriods, baseline.macdFastPeriod(), "MACD fast");
        requireBaseline(macdSlowPeriods, baseline.macdSlowPeriod(), "MACD slow");
        requireBaseline(macdSignalPeriods, baseline.macdSignalPeriod(), "MACD signal");
        requireBaseline(bBandsPeriods, baseline.bBandsPeriod(), "Bollinger Bands period");
        requireBaseline(bBandsMultipliers, baseline.bBandsMultiplier(), "Bollinger Bands multiplier");
        requireBaseline(adxPeriods, baseline.adxPeriod(), "ADX period");
        requireBaseline(adxThresholds, baseline.adxThreshold(), "ADX threshold");
    }

    private static <T> List<T> immutableNonEmpty(List<T> values, String name) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(name + " must contain at least one non-null value");
        }
        final List<T> copy = List.copyOf(values);
        if (copy.stream().distinct().count() != copy.size()) {
            throw new IllegalArgumentException(name + " must not contain duplicates");
        }
        return copy;
    }

    private static <T> void requireBaseline(List<T> candidates, T baseline, String name) {
        if (!candidates.contains(baseline)) {
            throw new IllegalArgumentException(name + " candidates must include the baseline value");
        }
    }

    /**
     * 基準値1件と、各項目について基準値以外へ変更する件数を合計する。
     * 実行前のレビューやログで探索規模を明示するために使用する。
     */
    public int candidateCount() {
        return 1
            + emaShortPeriods.size() - 1
            + emaLongPeriods.size() - 1
            + rsiPeriods.size() - 1
            + macdFastPeriods.size() - 1
            + macdSlowPeriods.size() - 1
            + macdSignalPeriods.size() - 1
            + bBandsPeriods.size() - 1
            + bBandsMultipliers.size() - 1
            + adxPeriods.size() - 1
            + adxThresholds.size() - 1;
    }
}
