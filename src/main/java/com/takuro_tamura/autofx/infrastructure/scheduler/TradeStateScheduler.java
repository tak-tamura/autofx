package com.takuro_tamura.autofx.infrastructure.scheduler;

import com.takuro_tamura.autofx.application.TradeStateApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeStateScheduler {

    private final TradeStateApplicationService tradeStateApplicationService;

    @Scheduled(cron = "0 0 21 * * FRI", zone = "Asia/Tokyo")
    public void suspendTradeForWeekend() {
        tradeStateApplicationService.suspendTrade();
    }

    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Tokyo")
    public void resumeTradeForWeekday() {
        tradeStateApplicationService.resumeTrade();
    }
}
