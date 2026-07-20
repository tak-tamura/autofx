package com.takuro_tamura.autofx.parametersearch.config;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 基準値から一度に1項目だけ変更し、各パラメータの寄与を比較できる候補を生成する。
 */
public final class StrategyParameterCandidateGenerator {

    public List<StrategyParameterSet> generate(StrategySearchSpace searchSpace) {
        if (searchSpace == null) {
            throw new IllegalArgumentException("Strategy search space is required");
        }
        if (searchSpace.mode() != SearchMode.ONE_FACTOR_AT_A_TIME) {
            throw new IllegalArgumentException("Unsupported search mode: " + searchSpace.mode());
        }
        if (searchSpace.candidateCount() > searchSpace.maxCandidates()) {
            throw new IllegalArgumentException(
                "Generated candidate count " + searchSpace.candidateCount()
                    + " exceeds limit " + searchSpace.maxCandidates()
            );
        }

        final StrategyParameterSet baseline = searchSpace.baseline();
        final Set<StrategyParameterSet> candidates = new LinkedHashSet<>();
        candidates.add(baseline);

        // 各リストを独立に展開し、他の値は基準値に固定して組み合わせ爆発を防ぐ。
        add(candidates, searchSpace.emaShortPeriods(), value -> copy(baseline, value, null, null, null, null, null, null, null, null, null));
        add(candidates, searchSpace.emaLongPeriods(), value -> copy(baseline, null, value, null, null, null, null, null, null, null, null));
        add(candidates, searchSpace.rsiPeriods(), value -> copy(baseline, null, null, value, null, null, null, null, null, null, null));
        add(candidates, searchSpace.macdFastPeriods(), value -> copy(baseline, null, null, null, value, null, null, null, null, null, null));
        add(candidates, searchSpace.macdSlowPeriods(), value -> copy(baseline, null, null, null, null, value, null, null, null, null, null));
        add(candidates, searchSpace.macdSignalPeriods(), value -> copy(baseline, null, null, null, null, null, value, null, null, null, null));
        add(candidates, searchSpace.bBandsPeriods(), value -> copy(baseline, null, null, null, null, null, null, value, null, null, null));
        add(candidates, searchSpace.bBandsMultipliers(), value -> copy(baseline, null, null, null, null, null, null, null, value, null, null));
        add(candidates, searchSpace.adxPeriods(), value -> copy(baseline, null, null, null, null, null, null, null, null, value, null));
        add(candidates, searchSpace.adxThresholds(), value -> copy(baseline, null, null, null, null, null, null, null, null, null, value));

        if (candidates.size() > searchSpace.maxCandidates()) {
            throw new IllegalArgumentException(
                "Generated candidate count " + candidates.size() + " exceeds limit " + searchSpace.maxCandidates()
            );
        }
        return List.copyOf(candidates);
    }

    private static <T> void add(
        Set<StrategyParameterSet> candidates,
        List<T> values,
        Function<T, StrategyParameterSet> candidateFactory
    ) {
        for (T value : values) {
            // StrategyParameterSetの生成時にEMA/MACDの大小関係を含む依存制約も検証される。
            candidates.add(candidateFactory.apply(value));
        }
    }

    private static StrategyParameterSet copy(
        StrategyParameterSet baseline,
        Integer emaShort,
        Integer emaLong,
        Integer rsi,
        Integer macdFast,
        Integer macdSlow,
        Integer macdSignal,
        Integer bBandsPeriod,
        BigDecimal bBandsMultiplier,
        Integer adxPeriod,
        BigDecimal adxThreshold
    ) {
        return new StrategyParameterSet(
            emaShort == null ? baseline.emaShortPeriod() : emaShort,
            emaLong == null ? baseline.emaLongPeriod() : emaLong,
            rsi == null ? baseline.rsiPeriod() : rsi,
            macdFast == null ? baseline.macdFastPeriod() : macdFast,
            macdSlow == null ? baseline.macdSlowPeriod() : macdSlow,
            macdSignal == null ? baseline.macdSignalPeriod() : macdSignal,
            bBandsPeriod == null ? baseline.bBandsPeriod() : bBandsPeriod,
            bBandsMultiplier == null ? baseline.bBandsMultiplier() : bBandsMultiplier,
            adxPeriod == null ? baseline.adxPeriod() : adxPeriod,
            adxThreshold == null ? baseline.adxThreshold() : adxThreshold
        );
    }
}
