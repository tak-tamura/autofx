package com.takuro_tamura.autofx.domain.indicator;

import com.takuro_tamura.autofx.presentation.controller.response.BBandsRecord;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.Getter;

@Getter
public class BBands {
    private final int n;
    private final double k;
    private final double[] up;
    private final double[] mid;
    private final double[] down;

    public BBands(int n, double k, double[] closePrices) {
        this.n = n;
        this.k = k;
        this.up = new double[closePrices.length];
        this.mid = new double[closePrices.length];
        this.down = new double[closePrices.length];
        generateBBands(closePrices);
    }

    private void generateBBands(double[] closePrices) {
        final Core core = new Core();
        final int startIndex = 0;
        final int endIndex = closePrices.length - 1;
        final MInteger begin = new MInteger();
        final MInteger length = new MInteger();
        final double[] tmpUp = new double[closePrices.length];
        final double[] tmpMid = new double[closePrices.length];
        final double[] tmpDown = new double[closePrices.length];

        final RetCode ret = core.bbands(
            startIndex,
            endIndex,
            closePrices,
            this.n,
            this.n,
            this.k,
            MAType.Sma,
            begin,
            length,
            tmpUp,
            tmpMid,
            tmpDown
        );

        if (ret != RetCode.Success) {
            throw new IllegalArgumentException("Invalid input for bbands, ret:" + ret);
        }

        System.arraycopy(tmpUp, 0, this.up, begin.value, length.value);
        System.arraycopy(tmpMid, 0, this.mid, begin.value, length.value);
        System.arraycopy(tmpDown, 0, this.down, begin.value, length.value);
    }

    public BBandsRecord toRecord() {
        return new BBandsRecord(this.n, this.k, this.up, this.mid, this.down);
    }

    public boolean shouldBuy(double[] closePrices) {
        if (up.length <= this.n) {
            return false;
        }
        return shouldBuy(up.length - 1, closePrices);
    }

    public boolean shouldBuy(int index, double[] closePrices) {
        if (index < 1) {
            throw new IllegalArgumentException("index should be more than 0");
        }

        return down[index-1] > closePrices[index-1] && down[index] <= closePrices[index];
    }

    public boolean shouldSell(double[] closePrices) {
        if (up.length <= this.n) {
            return false;
        }
        return shouldSell(up.length - 1, closePrices);
    }

    public boolean shouldSell(int index, double[] closePrices) {
        if (index < 1) {
            throw new IllegalArgumentException("index should be more than 0");
        }

        return up[index-1] < closePrices[index-1] && up[index] >= closePrices[index];
    }
}
