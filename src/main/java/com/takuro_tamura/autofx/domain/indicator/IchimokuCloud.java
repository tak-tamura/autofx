package com.takuro_tamura.autofx.domain.indicator;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.presentation.controller.response.IchimokuRecord;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public class IchimokuCloud {
    private double[] tenkan;
    private double[] kijun;
    private double[] senkouA;
    private double[] senkouB;
    private double[] chikou;


    public IchimokuCloud(double[] closePrices) {
        generate(closePrices);
    }

    private void generate(double[] closePrices) {
        final List<Double> tmpTenkan = new ArrayList<>(Collections.nCopies(Math.min(9, closePrices.length), 0.0));
        final List<Double> tmpKijun = new ArrayList<>(Collections.nCopies(Math.min(26, closePrices.length), 0.0));
        final List<Double> tmpSenkouA = new ArrayList<>(Collections.nCopies(Math.min(26, closePrices.length), 0.0));
        final List<Double> tmpSenkouB = new ArrayList<>(Collections.nCopies(Math.min(52, closePrices.length), 0.0));
        final List<Double> tmpChikou = new ArrayList<>(Collections.nCopies(Math.min(26, closePrices.length), 0.0));

        for (int i = 0; i < closePrices.length; i++) {
            if (i >= 9) {
                final Pair<Double, Double> minMax = extractMinMax(Arrays.copyOfRange(closePrices, i - 9, i));
                tmpTenkan.add((minMax.getLeft() + minMax.getRight()) / 2);
            }
            if (i >= 26) {
                final Pair<Double, Double> minMax = extractMinMax(Arrays.copyOfRange(closePrices, i - 26, i));
                tmpKijun.add((minMax.getLeft() + minMax.getRight()) / 2);
                tmpSenkouA.add((tmpTenkan.get(i) + tmpKijun.get(i)) / 2);
                tmpChikou.add(closePrices[i - 26]);
            }
            if (i >= 52) {
                final Pair<Double, Double> minMax = extractMinMax(Arrays.copyOfRange(closePrices, i - 52, i));
                tmpSenkouB.add((minMax.getLeft() + minMax.getRight()) / 2);
            }
        }

        this.tenkan = ArrayUtils.toPrimitive(tmpTenkan.toArray(new Double[0]));
        this.kijun = ArrayUtils.toPrimitive(tmpKijun.toArray(new Double[0]));
        this.senkouA = ArrayUtils.toPrimitive(tmpSenkouA.toArray(new Double[0]));
        this.senkouB = ArrayUtils.toPrimitive(tmpSenkouB.toArray(new Double[0]));
        this.chikou = ArrayUtils.toPrimitive(tmpChikou.toArray(new Double[0]));
    }

    public IchimokuRecord toRecord() {
        return new IchimokuRecord(tenkan, kijun, senkouA, senkouB, chikou);
    }

    public boolean shouldBuy(double high, double low) {
        if (chikou.length < 2) {
            return false;
        }
        return shouldBuy(chikou.length - 1, high, low);
    }

    public boolean shouldBuy(int index, double high, double low) {
        return chikou[index-1] < high  && chikou[index]  >= high &&
               senkouA[index]  < low   && senkouB[index] <  low  &&
               tenkan[index]   > kijun[index];
    }

    public boolean shouldSell(double high, double low) {
        if (chikou.length < 2) {
            return false;
        }
        return shouldSell(chikou.length - 1, high, low);
    }

    public boolean shouldSell(int index, double high, double low) {
        return chikou[index-1] > low  && chikou[index]  <= low   &&
               senkouA[index]  > high && senkouB[index] >  high  &&
               tenkan[index]   < kijun[index];
    }

    private Pair<Double, Double> extractMinMax(double[] closePrices) {
        double min = closePrices[0];
        double max = closePrices[0];
        for (double v : closePrices) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return Pair.of(min, max);
    }
}
