package com.takuro_tamura.autofx.application.state;

import com.takuro_tamura.autofx.application.TradeStateApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * トレード状態のスケジューリング管理
 * 指定時刻にトレード状態の変更をスケジュール
 */
@Component
@Slf4j
public class TradeScheduler {

    private final TaskScheduler taskScheduler;
    private final TradeStateApplicationService tradeStateApplicationService;

    public TradeScheduler(TradeStateApplicationService tradeStateApplicationService) {
        this.tradeStateApplicationService = tradeStateApplicationService;

        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    /**
     * 指定時刻にトレード停止をスケジュール
     * @param time スケジュール実行時刻
     */
    public void scheduleSuspendTrade(LocalDateTime time) {
        taskScheduler.schedule(() -> {
            log.info("Suspend trade by scheduled task");
            tradeStateApplicationService.suspendTrade();
        }, time.atZone(ZoneId.of("Asia/Tokyo")).toInstant());
        log.info("Scheduled trade suspension at {}", time);
    }

    /**
     * 指定時刻にトレード再開をスケジュール
     * @param time スケジュール実行時刻
     */
    public void scheduleResumeTrade(LocalDateTime time) {
        taskScheduler.schedule(() -> {
            log.info("Resume trade by scheduled task");
            tradeStateApplicationService.resumeTrade();
        }, time.atZone(ZoneId.of("Asia/Tokyo")).toInstant());
        log.info("Scheduled trade resume at {}", time);
    }
}
