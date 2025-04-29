package com.takuro_tamura.autofx.domain.indicator;

import com.takuro_tamura.autofx.presentation.controller.response.RsiRecord;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class Rsi {
    private final int period;
    private final double[] values;

    public Rsi(int period, double[] closePrices) {
        this.period = period;
        this.values = new double[closePrices.length];
        generate(closePrices);
    }

    private void generate(double[] closePrices) {
        final Core core = new Core();
        final int startIndex = 0;
        final int endIndex = closePrices.length - 1;
        final MInteger begin = new MInteger();
        final MInteger length = new MInteger();
        final double[] tmpRsi = new double[closePrices.length];

        final RetCode ret = core.rsi(
            startIndex,
            endIndex,
            closePrices,
            this.period,
            begin,
            length,
            tmpRsi
        );

        if (ret != RetCode.Success) {
            throw new IllegalArgumentException("Invalid input for rsi, ret:" + ret);
        }

        System.arraycopy(tmpRsi, 0, this.values, begin.value, length.value);
    }

    public RsiRecord toRecord() {
        return new RsiRecord(period, values);
    }

    public boolean shouldBuy(int buyThread) {
        if (values.length < 2) {
            return false;
        }
        return shouldBuy(values.length - 1, buyThread);
    }

    public boolean shouldBuy(int index, int buyThread) {
        if (values[index-1] == 0 || values[index-1] == 100) {
            return false;
        }
        return values[index-1] < buyThread && values[index] >= buyThread;
    }

    public boolean shouldSell(int sellThread) {
        if (values.length < 2) {
            return false;
        }
        return shouldSell(values.length - 1, sellThread);
    }

    public boolean shouldSell(int index, int sellThread) {
        if (values[index-1] == 0 || values[index-1] == 100) {
            return false;
        }
        return values[index-1] > sellThread && values[index] <= sellThread;
    }
}
