package com.takuro_tamura.autofx.domain.strategy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdxEntryFilter {

    private final double adxThreshold;

    public boolean canEnter(double[] adxValues) {
        if (adxValues == null || adxValues.length == 0) {
            throw new IllegalArgumentException("ADX values are required for entry filter");
        }

        double adxNow = adxValues[adxValues.length - 1];

        // ADXが計算できていない領域は見送り
        if (Double.isNaN(adxNow)) {
            return false;
        }

        // フィルターロジック
        return adxNow >= adxThreshold;
    }
}
