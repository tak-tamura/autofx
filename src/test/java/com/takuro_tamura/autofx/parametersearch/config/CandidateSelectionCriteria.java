package com.takuro_tamura.autofx.parametersearch.config;

import java.math.BigDecimal;

/**
 * In-sample結果だけで候補を絞り込むための事前定義済み基準。
 *
 * <p>最低取引数でサンプル不足を除外し、ネット利益、PF、平均Rで候補の最低品質を定める。
 * {@code maximumSelectedCandidates}は、基準を通過した候補のうちOut-of-sample評価へ進める件数の上限を表す。</p>
 */
public record CandidateSelectionCriteria(
    int minimumTrades,
    BigDecimal minimumNetProfit,
    BigDecimal minimumProfitFactor,
    BigDecimal minimumAverageR,
    int maximumSelectedCandidates
) {
    /** 不完全な基準で探索結果を選定しないよう、生成時に全閾値を検証する。 */
    public CandidateSelectionCriteria {
        if (minimumTrades <= 0 || maximumSelectedCandidates <= 0) {
            throw new IllegalArgumentException("Trade and selected-candidate counts must be greater than zero");
        }
        if (minimumNetProfit == null || minimumProfitFactor == null || minimumAverageR == null) {
            throw new IllegalArgumentException("All candidate-selection thresholds are required");
        }
        if (minimumProfitFactor.signum() < 0) {
            throw new IllegalArgumentException("Minimum profit factor must not be negative");
        }
    }
}
