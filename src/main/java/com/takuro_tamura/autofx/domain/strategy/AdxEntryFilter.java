package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class AdxEntryFilter {

    private final int adxPeriod;
    private final double adxThreshold;

    public boolean canEnter(List<Candle> candles, double[] adxValues) {
        int n = candles.size();
        if (n < adxPeriod + 5) {
            return false; // Not enough data to calculate ADX
        }

        int lastIndex = n - 1;
        int prevIndex = n - 2;

        double adxNow = adxValues[lastIndex];
        double adxPrev = adxValues[prevIndex];

        // ADXが計算できていない領域は見送り
        if (Double.isNaN(adxNow) || Double.isNaN(adxPrev)) {
            return false;
        }

        // フィルターロジック
        boolean strongTrend = adxNow >= adxThreshold;
        boolean risingTrend = adxNow > adxPrev;

        return strongTrend && risingTrend;
    }
}
