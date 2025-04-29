package com.takuro_tamura.autofx.presentation.controller.response;

public record MacdRecord(
    int fastPeriod,
    int slowPeriod,
    int signalPeriod,
    double[] macd,
    double[] macdSignal,
    double[] macdHist
) {
}
