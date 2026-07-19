package com.takuro_tamura.autofx.domain.backtest;

import com.takuro_tamura.autofx.domain.model.entity.Order;

import java.time.LocalDateTime;

public record BacktestTrade(
    Order order,
    LocalDateTime signalDatetime,
    BacktestExitReason exitReason
) {
    // Order自体の決済状態を更新した後、immutableな台帳情報へ決済理由を反映する。
    public BacktestTrade withExitReason(BacktestExitReason reason) {
        return new BacktestTrade(order, signalDatetime, reason);
    }
}
