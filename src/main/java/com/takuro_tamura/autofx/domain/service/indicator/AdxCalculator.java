package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.model.entity.Candle;

import java.util.List;

public class AdxCalculator {

    public static double[] calculateAdx(List<Candle> candles, int period) {
        final int n = candles.size();
        if (n <= period) {
            throw new IllegalArgumentException("Not enough candles to calculate ADX");
        }

        double[] tr = new double[n];
        double[] plusDm = new double[n];
        double[] minusDm = new double[n];

        // 1. TR, +DM, -DMを算出
        for (int i = 1; i < n; i++) {
            final Candle prev = candles.get(i - 1);
            final Candle curr = candles.get(i);

            final double highDiff = curr.getHigh().getValue().subtract(prev.getHigh().getValue()).doubleValue();
            final double lowDiff = prev.getLow().getValue().subtract(curr.getLow().getValue()).doubleValue();

            // True Range
            final double tr1 = curr.getHigh().getValue().subtract(curr.getLow().getValue()).doubleValue();
            final double tr2 = Math.abs(curr.getHigh().getValue().subtract(prev.getClose().getValue()).doubleValue());
            final double tr3 = Math.abs(curr.getLow().getValue().subtract(prev.getClose().getValue()).doubleValue());
            tr[i] = Math.max(tr1, Math.max(tr2, tr3));

            // +DM / -DM
            final double pdm = highDiff > lowDiff && highDiff > 0 ? highDiff : 0.0;
            final double mdm = lowDiff > highDiff && lowDiff > 0 ? lowDiff : 0.0;

            plusDm[i] = pdm;
            minusDm[i] = mdm;
        }

        double[] atr = new double[n];
        double[] plusDi = new double[n];
        double[] minusDi = new double[n];
        double[] dx = new double[n];
        double[] adx = new double[n];

        for (int i = 0; i < n; i++) {
            atr[i] = plusDi[i] = minusDi[i] = dx[i] = adx[i] = 0.0;
        }

        // 2. 最初のperiod分は単純合計でATR/+DI/-DIを算出
        double atrSum = 0.0;
        double plusDmSum = 0.0;
        double minusDmSum = 0.0;

        for (int i = 1; i <= period; i++) {
            atrSum += tr[i];
            plusDmSum += plusDm[i];
            minusDmSum += minusDm[i];
        }

        atr[period] = atrSum / period;
        double plusDmSmoothed = plusDmSum;
        double minusDmSmoothed = minusDmSum;

        // 3. Wilderの平滑化
        for (int i = period + 1; i < n; i++) {
            // ATR
            atr[i] = (atr[i - 1] * (period - 1) + tr[i]) / period;

            // +DM / -DM
            plusDmSmoothed = plusDmSmoothed - (plusDmSmoothed / period) + plusDm[i];
            minusDmSmoothed = minusDmSmoothed - (minusDmSmoothed / period) + minusDm[i];

            plusDi[i] = (plusDmSmoothed / atr[i]) * 100.0;
            minusDi[i] = (minusDmSmoothed / atr[i]) * 100.0;

            dx[i] = (Math.abs(plusDi[i] - minusDi[i]) / (plusDi[i] + minusDi[i])) * 100.0;
        }

        // 4. 最初のADXをDXの平均で算出
        double dxSum = 0.0;
        int dxStart = period + 1;
        int dxEnd = period * 2;

        if (dxEnd >= n) {
            dxEnd = n - 1;
        }

        int dxCount = 0;
        for (int i = dxStart; i <= dxEnd; i++) {
            if (!Double.isNaN(dx[i])) {
                dxSum += dx[i];
                dxCount++;
            }
        }
        int firstAdxIndex = dxEnd;
        adx[firstAdxIndex] = dxSum / dxCount;

        // 5. 残りはWilderの平滑化
        for (int i = firstAdxIndex + 1; i < n; i++) {
            adx[i] = ((adx[i - 1] * (period - 1)) + dx[i]) / period;
        }

        return adx;
    }
}
