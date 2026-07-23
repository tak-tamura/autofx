package com.takuro_tamura.autofx.parametersearch.walkforward;

/** 固定候補がウォークフォワード安定性基準を満たさなかった理由。 */
public enum WalkForwardRejectionReason {
    INSUFFICIENT_TRADES_IN_ONE_OR_MORE_WINDOWS,
    PROFITABLE_WINDOW_RATE_BELOW_MINIMUM,
    POSITIVE_AVERAGE_R_WINDOW_RATE_BELOW_MINIMUM
}
