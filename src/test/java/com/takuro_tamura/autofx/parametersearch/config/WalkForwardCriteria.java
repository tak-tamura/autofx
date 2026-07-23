package com.takuro_tamura.autofx.parametersearch.config;

import java.math.BigDecimal;

/** OOS期間を連続ウィンドウへ分割し、候補の期間安定性を判定する固定基準。 */
public record WalkForwardCriteria(
    int windowMonths,
    int minimumTradesPerWindow,
    BigDecimal minimumProfitableWindowRate,
    BigDecimal minimumPositiveAverageRWindowRate
) {
    public WalkForwardCriteria {
        if (windowMonths <= 0 || minimumTradesPerWindow <= 0) {
            throw new IllegalArgumentException("Walk-forward window months and minimum trades must be positive");
        }
        validateRate(minimumProfitableWindowRate, "Minimum profitable-window rate");
        validateRate(minimumPositiveAverageRWindowRate, "Minimum positive-average-R window rate");
    }

    private static void validateRate(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between zero and one");
        }
    }
}
