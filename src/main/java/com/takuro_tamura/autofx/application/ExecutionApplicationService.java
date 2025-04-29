package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.Execution;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionApplicationService {
    private final Logger log = LoggerFactory.getLogger(ExecutionApplicationService.class);
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RedisCacheService redisCacheService;

    public void handleExecution(Execution execution) {
        switch (execution.getSettleType()) {
            case OPEN -> handleOpenExecution(execution);
            case CLOSE -> handleCloseExecution(execution);
        }
    }

    /**
     * 新規オーダーの約定イベント処理
     * @param execution
     */
    private void handleOpenExecution(Execution execution) {
        // オーダー情報をDBに保存
        final Order order = Order.newOrder()
            .orderId(execution.getOrderId())
            .currencyPair(execution.getCurrencyPair())
            .side(execution.getSide())
            .size(execution.getSize())
            .fillDatetime(execution.getExecutionTime())
            .price(new Price(execution.getPrice()))
            .build();
        log.info("Saving new order: {}", order);
        orderRepository.save(order);

        // 損切りと利確注文の送信
        try {
            // POST APIは秒間1件しか呼び出せないのでsleepする
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {}
        orderService.makeStopAndProfitOrder(order);
    }

    /**
     * 決済オーダーの約定イベント処理
     * @param execution
     */
    private void handleCloseExecution(Execution execution) {
        // 新規注文時のIDをキャッシュから取得
        final String cacheKey = CacheKey.CLOSE_ORDER_ID.build(String.valueOf(execution.getOrderId()));
        log.info("Get orderId from cache with key({})", execution.getOrderId());

        // 成行注文で決済注文した場合はorderIdのキャッシュが間に合わないのでsleepする
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {}

        final Long orderId = redisCacheService.<Long>get(cacheKey)
            .orElseThrow(() -> new IllegalStateException("Cannot find root order id from cache"));
        redisCacheService.delete(cacheKey);

        // DB上のオーダー情報を更新
        final Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("Cannot find order from DB"));
        order.close(LocalDateTime.now(), new Price(execution.getPrice()));
        orderRepository.update(order);
        log.info("Order updated: {}", order);

        // 成行注文で決済した場合はOCO注文のオーダーIDがRedisに残るので削除する
        redisCacheService.deleteKeysByValue(order.getOrderId());
    }
}
