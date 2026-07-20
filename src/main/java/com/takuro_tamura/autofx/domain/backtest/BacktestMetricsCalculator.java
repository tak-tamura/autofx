package com.takuro_tamura.autofx.domain.backtest;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 決済済みの取引台帳を時系列に集計し、パラメータ探索で比較可能な指標へ変換する。
 */
public class BacktestMetricsCalculator {
    private static final int DIVISION_SCALE = 8;

    public BacktestMetrics calculate(
        BacktestResult result,
        LocalDateTime datasetStart,
        LocalDateTime datasetEnd
    ) {
        validateInputs(result, datasetStart, datasetEnd);
        validateTransactionCostsAreApplied(result.assumptions());

        int wins = 0;
        int losses = 0;
        int breakEvens = 0;
        int currentWins = 0;
        int currentLosses = 0;
        int maximumWins = 0;
        int maximumLosses = 0;
        long exposureMillis = 0;
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        BigDecimal equity = BigDecimal.ZERO;
        BigDecimal equityPeak = BigDecimal.ZERO;
        BigDecimal maximumDrawdown = BigDecimal.ZERO;
        final List<BigDecimal> rMultiples = new ArrayList<>();
        LocalDateTime previousClose = null;

        for (BacktestTrade trade : result.trades()) {
            final Order order = validateTrade(trade, datasetStart, datasetEnd);
            if (previousClose != null && order.getFillDatetime().isBefore(previousClose)) {
                throw new IllegalArgumentException("Trades must be ordered and must not overlap");
            }
            previousClose = order.getCloseDatetime();
            final BigDecimal profit = order.calculateProfit();
            equity = equity.add(profit);
            if (equity.compareTo(equityPeak) > 0) {
                equityPeak = equity;
            }
            maximumDrawdown = maximumDrawdown.max(equityPeak.subtract(equity));

            if (profit.signum() > 0) {
                wins++;
                currentWins++;
                currentLosses = 0;
                maximumWins = Math.max(maximumWins, currentWins);
                grossProfit = grossProfit.add(profit);
            } else if (profit.signum() < 0) {
                losses++;
                currentLosses++;
                currentWins = 0;
                maximumLosses = Math.max(maximumLosses, currentLosses);
                grossLoss = grossLoss.add(profit.abs());
            } else {
                breakEvens++;
                currentWins = 0;
                currentLosses = 0;
            }

            rMultiples.add(calculateRMultiple(order, profit));
            exposureMillis = Math.addExact(
                exposureMillis,
                Duration.between(order.getFillDatetime(), order.getCloseDatetime()).toMillis()
            );
        }

        final int tradeCount = result.trades().size();
        final long datasetMillis = Duration.between(datasetStart, datasetEnd).toMillis();
        if (exposureMillis > datasetMillis) {
            throw new IllegalArgumentException("Trade exposure exceeds dataset duration");
        }

        return new BacktestMetrics(
            tradeCount,
            wins,
            losses,
            breakEvens,
            ratio(wins, tradeCount),
            grossProfit,
            grossLoss,
            grossProfit.subtract(grossLoss),
            average(grossProfit, wins),
            average(grossLoss, losses),
            losses == 0 ? Optional.empty() : Optional.of(divide(grossProfit, grossLoss)),
            maximumDrawdown,
            maximumWins,
            maximumLosses,
            rMultiples,
            rMultiples.isEmpty()
                ? Optional.empty()
                : Optional.of(average(rMultiples)),
            divide(BigDecimal.valueOf(exposureMillis), BigDecimal.valueOf(datasetMillis)),
            BigDecimal.ZERO
        );
    }

    private void validateInputs(BacktestResult result, LocalDateTime datasetStart, LocalDateTime datasetEnd) {
        if (result == null || datasetStart == null || datasetEnd == null) {
            throw new IllegalArgumentException("Backtest result and dataset period are required");
        }
        if (!datasetStart.isBefore(datasetEnd)) {
            throw new IllegalArgumentException("Dataset start must be before dataset end");
        }
    }

    private void validateTransactionCostsAreApplied(BacktestAssumptions assumptions) {
        if (assumptions == null) {
            throw new IllegalArgumentException("Backtest assumptions are required");
        }
        if (assumptions.spread().signum() != 0
            || assumptions.slippage().signum() != 0
            || assumptions.commission().signum() != 0) {
            throw new IllegalArgumentException(
                "Non-zero transaction costs must be applied to each trade before calculating metrics"
            );
        }
    }

    private Order validateTrade(
        BacktestTrade trade,
        LocalDateTime datasetStart,
        LocalDateTime datasetEnd
    ) {
        if (trade == null || trade.order() == null) {
            throw new IllegalArgumentException("Backtest trade and order are required");
        }
        final Order order = trade.order();
        if (order.getStatus() != OrderStatus.CLOSED
            || order.getFillDatetime() == null || order.getCloseDatetime() == null
            || order.getFillPrice() == null || order.getClosePrice() == null) {
            throw new IllegalArgumentException("Metrics require a fully closed trade");
        }
        if (trade.exitReason() == null) {
            throw new IllegalArgumentException("Metrics require a recorded exit reason");
        }
        if (order.getProtectionLevels() == null) {
            throw new IllegalArgumentException("Metrics require protection levels fixed at entry");
        }
        if (order.getFillDatetime().isBefore(datasetStart)
            || order.getCloseDatetime().isAfter(datasetEnd)
            || order.getCloseDatetime().isBefore(order.getFillDatetime())) {
            throw new IllegalArgumentException("Trade timestamps are outside the dataset period");
        }
        return order;
    }

    private BigDecimal calculateRMultiple(Order order, BigDecimal profit) {
        final BigDecimal stopDistance = order.getFillPrice().getValue()
            .subtract(order.getProtectionLevels().stopPrice().getValue())
            .abs();
        final BigDecimal initialRisk = stopDistance.multiply(BigDecimal.valueOf(order.getSize()));
        if (initialRisk.signum() <= 0) {
            throw new IllegalArgumentException("Initial trade risk must be greater than zero");
        }
        return divide(profit, initialRisk);
    }

    private BigDecimal ratio(int numerator, int denominator) {
        return denominator == 0
            ? BigDecimal.ZERO
            : divide(BigDecimal.valueOf(numerator), BigDecimal.valueOf(denominator));
    }

    private BigDecimal average(BigDecimal total, int count) {
        return count == 0 ? BigDecimal.ZERO : divide(total, BigDecimal.valueOf(count));
    }

    private BigDecimal average(List<BigDecimal> values) {
        final BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return divide(total, BigDecimal.valueOf(values.size()));
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, DIVISION_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
