package com.takuro_tamura.autofx.domain.indicator;

import com.takuro_tamura.autofx.presentation.controller.response.MaRecord;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Ema {
    private final int[] periods;
    private final double[][] values;

    public Ema(int[] periods, double[] closePrices) {
        this.periods = periods;
        this.values = new double[periods.length][closePrices.length];
        generateEma(closePrices);
    }

    private void generateEma(double[] closePrices) {
        final Core core = new Core();
        final int startIndex = 0;
        final int endIndex = closePrices.length - 1;

        for (int i = 0; i < this.periods.length; i++) {
            final MInteger begin = new MInteger();
            final MInteger length = new MInteger();
            final double[] tmp = new double[closePrices.length];
            final RetCode ret = core.ema(
                startIndex,
                endIndex,
                closePrices,
                periods[i],
                begin,
                length,
                tmp
            );

            if (ret != RetCode.Success) {
                throw new IllegalArgumentException("Invalid input for sma, ret:" + ret);
            }

            System.arraycopy(tmp, 0, values[i], begin.value, length.value);
        }
    }

    public List<MaRecord> toRecords() {
        final List<MaRecord> result = new ArrayList<>(periods.length);
        for (int i = 0; i < periods.length; i++) {
            result.add(new MaRecord(periods[i], values[i]));
        }
        return result;
    }

    public boolean shouldBuy() {
        if (values[0].length <= periods[0] || values[1].length <= periods[1]) {
            return false;
        }
        return shouldBuy(values[0].length - 1);
    }

    public boolean shouldBuy(int index) {
        if (index < 1) {
            throw new IllegalArgumentException("index should be more than 0");
        }

        if (values.length < 2) {
            throw new IllegalArgumentException("at least 2 periods of emas to get signal");
        }

        return values[0][index-1] < values[1][index-1] && values[0][index] >= values[1][index];
    }

    public boolean shouldSell() {
        if (values[0].length <= periods[0] || values[1].length <= periods[1]) {
            return false;
        }
        return shouldSell(values[0].length - 1);
    }

    public boolean shouldSell(int index) {
        if (index < 1) {
            throw new IllegalArgumentException("index should be more than 0");
        }

        if (values.length < 2) {
            throw new IllegalArgumentException("at least 2 periods of emas to get signal");
        }

        return values[0][index-1] > values[1][index-1] && values[0][index] <= values[1][index];
    }
}
