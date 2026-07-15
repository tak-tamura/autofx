package com.takuro_tamura.autofx.domain.service.indicator;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AtrCalculatorTest {

    @Test
    void calculatesSimpleAverageAtrAtEvaluationIndex() {
        final List<Candle> candles = List.of(
            candle(0, "10", "11", "9"),
            candle(1, "12", "13", "11"),
            candle(2, "13", "14", "10")
        );

        assertThat(AtrCalculator.calculate(candles, 2, 2))
            .isEqualByComparingTo(new BigDecimal("3.5"));
    }

    @Test
    void ignoresCandlesAfterEvaluationIndex() {
        final List<Candle> candles = new ArrayList<>(List.of(
            candle(0, "10", "11", "9"),
            candle(1, "12", "13", "11"),
            candle(2, "13", "14", "10")
        ));
        final BigDecimal beforeAppendingFutureCandle = AtrCalculator.calculate(candles, 2, 2);

        candles.add(candle(3, "50", "100", "1"));

        assertThat(AtrCalculator.calculate(candles, 2, 2))
            .isEqualByComparingTo(beforeAppendingFutureCandle);
    }

    @Test
    void rejectsInsufficientHistoryAndInvalidPeriod() {
        final List<Candle> candles = List.of(
            candle(0, "10", "11", "9"),
            candle(1, "12", "13", "11")
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> AtrCalculator.calculate(candles, 1, 2));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> AtrCalculator.calculate(candles, 1, 0));
    }

    @Test
    void rejectsZeroAtr() {
        final List<Candle> candles = List.of(
            candle(0, "10", "10", "10"),
            candle(1, "10", "10", "10")
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> AtrCalculator.calculate(candles, 1, 1));
    }

    private Candle candle(int hour, String close, String high, String low) {
        return Candle.builder()
            .time(LocalDateTime.of(2026, 1, 1, hour, 0))
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price(close))
            .close(new Price(close))
            .high(new Price(high))
            .low(new Price(low))
            .build();
    }
}
