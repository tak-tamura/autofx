package com.takuro_tamura.autofx.presentation.controller.response;

public record IchimokuRecord(
    double[] tenkan,
    double[] kijun,
    double[] senkouA,
    double[] senkouB,
    double[] chikou
) {
}
