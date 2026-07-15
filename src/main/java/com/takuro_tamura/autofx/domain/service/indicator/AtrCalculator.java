package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.model.entity.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public final class AtrCalculator {

    private AtrCalculator() {
    }

    public static BigDecimal calculate(List<Candle> candles, int evaluationIndex, int period) {
        if (candles == null) {
            throw new IllegalArgumentException("Candles are required to calculate ATR");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("ATR period must be greater than zero");
        }
        if (evaluationIndex < 0 || evaluationIndex >= candles.size()) {
            throw new IllegalArgumentException("ATR evaluation index is out of bounds: " + evaluationIndex);
        }
        if (evaluationIndex < period) {
            throw new IllegalArgumentException(
                "Not enough candles to calculate ATR at index " + evaluationIndex + " for period " + period
            );
        }

        BigDecimal trueRangeSum = BigDecimal.ZERO;
        final int firstTrueRangeIndex = evaluationIndex - period + 1;

        for (int i = firstTrueRangeIndex; i <= evaluationIndex; i++) {
            final Candle previous = candles.get(i - 1);
            final Candle current = candles.get(i);

            final BigDecimal high = current.getHigh().getValue();
            final BigDecimal low = current.getLow().getValue();
            final BigDecimal previousClose = previous.getClose().getValue();

            final BigDecimal highLow = high.subtract(low).abs();
            final BigDecimal highPreviousClose = high.subtract(previousClose).abs();
            final BigDecimal lowPreviousClose = low.subtract(previousClose).abs();
            final BigDecimal trueRange = highLow.max(highPreviousClose).max(lowPreviousClose);

            trueRangeSum = trueRangeSum.add(trueRange);
        }

        final BigDecimal atr = trueRangeSum.divide(BigDecimal.valueOf(period), MathContext.DECIMAL64);
        if (atr.signum() <= 0) {
            throw new IllegalArgumentException("ATR must be greater than zero");
        }
        return atr;
    }
}
