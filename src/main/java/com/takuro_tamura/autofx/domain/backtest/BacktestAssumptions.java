package com.takuro_tamura.autofx.domain.backtest;

import java.math.BigDecimal;

public record BacktestAssumptions(
    String entryTiming,
    String intrabarExitModel,
    String bothTouchedPolicy,
    String finalPositionPolicy,
    BigDecimal spread,
    BigDecimal slippage,
    BigDecimal commission
) {
    /**
     * 結果の再現と旧バックテストとの識別に必要な約定仮定を返す。
     * 取引コストは未実装ではなく、現段階では明示的にゼロとして記録する。
     */
    public static BacktestAssumptions current() {
        return new BacktestAssumptions(
            "NEXT_CANDLE_OPEN",
            "OHLC_HIGH_LOW",
            "STOP_FIRST",
            "CLOSE_AT_FINAL_CLOSE",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }
}
