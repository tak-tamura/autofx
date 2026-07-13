package com.takuro_tamura.autofx.domain.strategy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdxEntryFilter {

    private final double adxThreshold;

    public boolean canEnter(double[] adxValues, int index) {
        if (adxValues == null || adxValues.length == 0) {
            throw new IllegalArgumentException("ADX values are required for entry filter");
        }
        if (index < 0 || index >= adxValues.length) {
            throw new IllegalArgumentException("ADX index is out of bounds: " + index);
        }

        double adxNow = adxValues[index];

        // ADXが計算できていない領域は見送り
        if (!Double.isFinite(adxNow)) {
            return false;
        }

        // フィルターロジック
        return adxNow >= adxThreshold;
    }
}
