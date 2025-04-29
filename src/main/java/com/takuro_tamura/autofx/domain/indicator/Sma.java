package com.takuro_tamura.autofx.domain.indicator;

import com.takuro_tamura.autofx.presentation.controller.response.MaRecord;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Sma {
    private final int[] periods;
    private final double[][] values;

    public Sma(int[] periods, double[] closePrices) {
        this.periods = periods;
        this.values = new double[periods.length][closePrices.length];
        generateSma(closePrices);
    }

    private void generateSma(double[] closePrices) {
        final Core core = new Core();
        final int startIndex = 0;
        final int endIndex = closePrices.length - 1;

        for (int i = 0; i < this.periods.length; i++) {
            final MInteger begin = new MInteger();
            final MInteger length = new MInteger();
            final double[] tmp = new double[closePrices.length];
            final RetCode ret = core.sma(
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
}
