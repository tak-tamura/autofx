package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TradeSettingApplicationService {
    private final Logger log = LoggerFactory.getLogger(TradeSettingApplicationService.class);
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RedisCacheService redisCacheService;
    private final TradeConfigParameterService tradeConfigParameterService;
    private final TaskScheduler taskScheduler;

    public TradeSettingApplicationService(
        OrderService orderService,
        OrderRepository orderRepository,
        RedisCacheService redisCacheService,
        TradeConfigParameterService tradeConfigParameterService
    ) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.redisCacheService = redisCacheService;
        this.tradeConfigParameterService = tradeConfigParameterService;

        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @Transactional
    public void suspendTrade() {
        redisCacheService.save(CacheKey.TRADE_ENABLED.getKey(), Boolean.FALSE);

        orderRepository.findLatestByCurrencyPairWithLock(tradeConfigParameterService.getTargetCurrencyPair())
            .ifPresent(order -> {
                if (order.getStatus() == OrderStatus.FILLED) {
                    orderService.closeOrder(order);
                }
            });
    }

    public void scheduleSuspendTrade(LocalDateTime time) {
        taskScheduler.schedule(() -> {
            log.info("Suspend trade by scheduled task");
            suspendTrade();
        }, time.atZone(ZoneId.of("Asia/Tokyo")).toInstant());
        log.info("Scheduled trade suspension at {}", time);
    }

    public void resumeTrade() {
        redisCacheService.save(CacheKey.TRADE_ENABLED.getKey(), Boolean.TRUE);
    }

    public void scheduleResumeTrade(LocalDateTime time) {
        taskScheduler.schedule(() -> {
            log.info("Resume trade by scheduled task");
            resumeTrade();
        }, time.atZone(ZoneId.of("Asia/Tokyo")).toInstant());
        log.info("Scheduled trade resume at {}", time);
    }
}
