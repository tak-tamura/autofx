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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BacktestMetricsCalculatorTest {
    private static final LocalDateTime DATASET_START = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime DATASET_END = DATASET_START.plusHours(8);

    private final BacktestMetricsCalculator calculator = new BacktestMetricsCalculator();

    @Test
    void calculatesStandardMetricsFromClosedTradeLedger() {
        final BacktestResult result = new BacktestResult(List.of(
            trade(OrderSide.BUY, 0, 2, "100", "104", "98", BacktestExitReason.TAKE_PROFIT),
            trade(OrderSide.BUY, 2, 3, "100", "99", "98", BacktestExitReason.STOP_LOSS),
            trade(OrderSide.SELL, 3, 5, "100", "102", "102", BacktestExitReason.STOP_LOSS),
            trade(OrderSide.SELL, 5, 6, "100", "99", "102", BacktestExitReason.END_OF_DATA)
        ), BacktestAssumptions.current());

        final BacktestMetrics metrics = calculator.calculate(result, DATASET_START, DATASET_END);

        assertThat(metrics.tradeCount()).isEqualTo(4);
        assertThat(metrics.winningTradeCount()).isEqualTo(2);
        assertThat(metrics.losingTradeCount()).isEqualTo(2);
        assertThat(metrics.breakEvenTradeCount()).isZero();
        assertThat(metrics.winRate()).isEqualByComparingTo("0.5");
        assertThat(metrics.grossProfit()).isEqualByComparingTo("500");
        assertThat(metrics.grossLoss()).isEqualByComparingTo("300");
        assertThat(metrics.netProfit()).isEqualByComparingTo("200");
        assertThat(metrics.averageWin()).isEqualByComparingTo("250");
        assertThat(metrics.averageLoss()).isEqualByComparingTo("150");
        assertThat(metrics.profitFactor()).contains(new BigDecimal("1.66666667"));
        assertThat(metrics.maximumDrawdown()).isEqualByComparingTo("300");
        assertThat(metrics.maximumConsecutiveWins()).isEqualTo(1);
        assertThat(metrics.maximumConsecutiveLosses()).isEqualTo(2);
        assertThat(metrics.rMultiples())
            .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .containsExactly(
                new BigDecimal("2"),
                new BigDecimal("-0.5"),
                new BigDecimal("-1"),
                new BigDecimal("0.5")
            );
        assertThat(metrics.averageR()).contains(new BigDecimal("0.25"));
        assertThat(metrics.exposure()).isEqualByComparingTo("0.75");
        assertThat(metrics.totalTransactionCosts()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void returnsExplicitEmptyMetricsWhenThereAreNoTrades() {
        final BacktestResult result = new BacktestResult(List.of(), BacktestAssumptions.current());

        final BacktestMetrics metrics = calculator.calculate(result, DATASET_START, DATASET_END);

        assertThat(metrics.tradeCount()).isZero();
        assertThat(metrics.winRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.maximumDrawdown()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.profitFactor()).isEmpty();
        assertThat(metrics.averageR()).isEmpty();
        assertThat(metrics.rMultiples()).isEmpty();
        assertThat(metrics.exposure()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void leavesProfitFactorUndefinedWhenThereAreNoLosingTrades() {
        final BacktestResult result = new BacktestResult(List.of(
            trade(OrderSide.BUY, 0, 1, "100", "102", "98", BacktestExitReason.TAKE_PROFIT)
        ), BacktestAssumptions.current());

        final BacktestMetrics metrics = calculator.calculate(result, DATASET_START, DATASET_END);

        assertThat(metrics.profitFactor()).isEmpty();
        assertThat(metrics.maximumConsecutiveWins()).isEqualTo(1);
        assertThat(metrics.maximumConsecutiveLosses()).isZero();
    }

    @Test
    void calculatesDrawdownWhenTheLargestLossSequenceEndsAtDatasetEnd() {
        final BacktestResult result = new BacktestResult(List.of(
            trade(OrderSide.BUY, 0, 1, "100", "99", "98", BacktestExitReason.STOP_LOSS),
            trade(OrderSide.SELL, 1, 3, "100", "102", "102", BacktestExitReason.END_OF_DATA)
        ), BacktestAssumptions.current());

        final BacktestMetrics metrics = calculator.calculate(result, DATASET_START, DATASET_END);

        assertThat(metrics.grossProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.grossLoss()).isEqualByComparingTo("300");
        assertThat(metrics.netProfit()).isEqualByComparingTo("-300");
        assertThat(metrics.profitFactor()).contains(BigDecimal.ZERO);
        assertThat(metrics.maximumDrawdown()).isEqualByComparingTo("300");
        assertThat(metrics.maximumConsecutiveLosses()).isEqualTo(2);
    }

    @Test
    void rejectsCostsThatHaveNotYetBeenAppliedToTradeProfit() {
        final BacktestAssumptions assumptions = new BacktestAssumptions(
            "NEXT_CANDLE_OPEN",
            "OHLC_HIGH_LOW",
            "STOP_FIRST",
            "CLOSE_AT_FINAL_CLOSE",
            new BigDecimal("0.01"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        final BacktestResult result = new BacktestResult(List.of(), assumptions);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> calculator.calculate(result, DATASET_START, DATASET_END))
            .withMessageContaining("must be applied");
    }

    private BacktestTrade trade(
        OrderSide side,
        int fillHour,
        int closeHour,
        String fillPrice,
        String closePrice,
        String stopPrice,
        BacktestExitReason exitReason
    ) {
        final Candle entryCandle = Candle.builder()
            .time(DATASET_START.plusHours(fillHour))
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price(fillPrice))
            .high(new Price(fillPrice))
            .low(new Price(fillPrice))
            .close(new Price(fillPrice))
            .build();
        final Order order = new Order(
            0L,
            CurrencyPair.USD_JPY,
            side,
            100,
            entryCandle.getTime(),
            entryCandle.getOpen()
        );
        final Price takeProfit = side == OrderSide.BUY ? new Price("110") : new Price("90");
        order.fixProtectionLevels(new ProtectionLevels(BigDecimal.ONE, new Price(stopPrice), takeProfit));
        order.close(DATASET_START.plusHours(closeHour), new Price(closePrice));
        return new BacktestTrade(order, entryCandle.getTime().minusHours(1), exitReason);
    }
}
