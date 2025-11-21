package com.takuro_tamura.autofx.infrastructure.scheduler;

import com.takuro_tamura.autofx.application.TradeStateApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeStateScheduler {

    private final TradeStateApplicationService tradeStateApplicationService;

    @Scheduled(cron = "0 0 21 * * FRI", zone = "Asia/Tokyo")
    public void suspendTradeForWeekend() {
        tradeStateApplicationService.suspendTrade();
        log.info("Suspended trade for weekend by scheduling task.");
    }

    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Tokyo")
    public void resumeTradeForWeekday() {
        tradeStateApplicationService.resumeTrade();
        log.info("Resumed trade for weekday by scheduling task.");
    }
}
