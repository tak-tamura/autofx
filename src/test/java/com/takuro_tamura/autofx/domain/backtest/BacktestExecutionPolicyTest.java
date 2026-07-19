package com.takuro_tamura.autofx.domain.backtest;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.ProtectionLevels;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestExecutionPolicyTest {
    private final BacktestExecutionPolicy policy = new BacktestExecutionPolicy();

    @Test
    void usesStopWhenLongStopAndTakeProfitAreBothTouched() {
        final Order order = protectedOrder(OrderSide.BUY);

        final var decision = policy.evaluateIntrabar(
            order,
            candle("100", "103", "97", "101")
        ).orElseThrow();

        assertThat(decision.price().getValue()).isEqualByComparingTo("98");
        assertThat(decision.reason()).isEqualTo(BacktestExitReason.BOTH_TOUCHED_STOP_FIRST);
    }

    @Test
    void detectsShortTakeProfitFromCandleLow() {
        final Order order = protectedOrder(OrderSide.SELL);

        final var decision = policy.evaluateIntrabar(
            order,
            candle("100", "101", "97", "99")
        ).orElseThrow();

        assertThat(decision.price().getValue()).isEqualByComparingTo("98");
        assertThat(decision.reason()).isEqualTo(BacktestExitReason.TAKE_PROFIT);
    }

    @Test
    void fillsLongStopGapAtAttainableOpenPrice() {
        final Order order = protectedOrder(OrderSide.BUY);

        final var decision = policy.evaluateGap(
            order,
            candle("97", "99", "96", "98")
        ).orElseThrow();

        assertThat(decision.price().getValue()).isEqualByComparingTo("97");
        assertThat(decision.reason()).isEqualTo(BacktestExitReason.STOP_LOSS);
    }

    private Order protectedOrder(OrderSide side) {
        final Candle entry = candle("100", "100", "100", "100");
        final Order order = new Order(0L, CurrencyPair.USD_JPY, side, 10000, entry.getTime(), entry.getOpen());
        final Price stop = side == OrderSide.BUY ? new Price("98") : new Price("102");
        final Price profit = side == OrderSide.BUY ? new Price("102") : new Price("98");
        order.fixProtectionLevels(new ProtectionLevels(BigDecimal.ONE, stop, profit));
        return order;
    }

    private Candle candle(String open, String high, String low, String close) {
        return Candle.builder()
            .time(LocalDateTime.of(2026, 1, 1, 0, 0))
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price(open))
            .high(new Price(high))
            .low(new Price(low))
            .close(new Price(close))
            .build();
    }
}
