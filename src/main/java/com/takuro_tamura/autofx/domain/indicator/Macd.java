package com.takuro_tamura.autofx.domain.indicator;

import com.takuro_tamura.autofx.presentation.controller.response.MacdRecord;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class Macd {
    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;
    private final double[] macd;
    private final double[] macdSignal;
    private final double[] macdHist;

    public Macd(int fastPeriod, int slowPeriod, int signalPeriod, double[] closePrices) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
        this.macd = new double[closePrices.length];
        this.macdSignal = new double[closePrices.length];
        this.macdHist = new double[closePrices.length];
        generate(closePrices);
    }

    private void generate(double[] closePrices) {
        final Core core = new Core();
        final int startIndex = 0;
        final int endIndex = closePrices.length - 1;
        final MInteger begin = new MInteger();
        final MInteger length = new MInteger();
        final double[] tmpMacd = new double[closePrices.length];
        final double[] tmpMacdSignal = new double[closePrices.length];
        final double[] tmpMacdHist = new double[closePrices.length];

        final RetCode ret = core.macd(
            startIndex,
            endIndex,
            closePrices,
            this.fastPeriod,
            this.slowPeriod,
            this.signalPeriod,
            begin,
            length,
            tmpMacd,
            tmpMacdSignal,
            tmpMacdHist
        );

        if (ret != RetCode.Success) {
            throw new IllegalArgumentException("Invalid input for rsi, ret:" + ret);
        }

        System.arraycopy(tmpMacd, 0, this.macd, begin.value, length.value);
        System.arraycopy(tmpMacdSignal, 0, this.macdSignal, begin.value, length.value);
        System.arraycopy(tmpMacdHist, 0, this.macdHist, begin.value, length.value);
    }

    public MacdRecord toRecord() {
        return new MacdRecord(fastPeriod, slowPeriod, signalPeriod, macd, macdSignal, macdHist);
    }

    public boolean shouldBuy() {
        if (macd.length < 2) {
            return false;
        }
        return shouldBuy(macd.length - 1);
    }

    public boolean shouldBuy(int index) {
        return macd[index]       <  0 &&
               macdSignal[index] <  0 &&
               macd[index-1]     <  macdSignal[index-1] &&
               macd[index]       >= macdSignal[index];
    }

    public boolean shouldSell() {
        if (macd.length < 2) {
            return false;
        }
        return shouldSell(macd.length - 1);
    }

    public boolean shouldSell(int index) {
        return macd[index]       >  0 &&
               macdSignal[index] >  0 &&
               macd[index-1]     >  macdSignal[index-1] &&
               macd[index]       <= macdSignal[index];
    }
}
