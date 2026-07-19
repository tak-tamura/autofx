package com.takuro_tamura.autofx.domain.backtest;

public enum BacktestExitReason {
    STOP_LOSS,
    TAKE_PROFIT,
    BOTH_TOUCHED_STOP_FIRST,
    OPPOSITE_SIGNAL,
    END_OF_DATA
}
