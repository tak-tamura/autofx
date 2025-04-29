package com.takuro_tamura.autofx.domain.parameter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class TradeParameter {
    private final Ema ema;
    private final BBands bBands;
    private final Ichimoku ichimoku;
    private final Macd macd;
    private final Rsi rsi;

    @RequiredArgsConstructor
    @Getter
    public static class Ema {
        private final boolean enable;
        private final int period1;
        private final int period2;
    }

    @RequiredArgsConstructor
    @Getter
    public static class BBands {
        private final boolean enable;
        private final int n;
        private final double k;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Ichimoku {
        private final boolean enable;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Macd {
        private final boolean enable;
        private final int fastPeriod;
        private final int slowPeriod;
        private final int signalPeriod;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Rsi {
        private final boolean enable;
        private final int period;
        private final int buyThread;
        private final int sellThread;
    }
}
