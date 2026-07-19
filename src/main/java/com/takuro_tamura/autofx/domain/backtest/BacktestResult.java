package com.takuro_tamura.autofx.domain.backtest;

import com.takuro_tamura.autofx.domain.model.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public record BacktestResult(
    List<BacktestTrade> trades,
    BacktestAssumptions assumptions
) {
    public BacktestResult {
        // 呼び出し元による取引一覧の追加・削除で集計結果が変わらないようにする。
        trades = List.copyOf(trades);
    }

    public List<Order> orders() {
        return trades.stream().map(BacktestTrade::order).toList();
    }

    public BigDecimal totalProfit() {
        return trades.stream()
            .map(BacktestTrade::order)
            .map(Order::calculateProfit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
