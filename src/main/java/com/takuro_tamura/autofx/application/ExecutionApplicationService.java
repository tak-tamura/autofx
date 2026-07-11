package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.Execution;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
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
    private final OrderCachePort orderCachePort;

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
        log.info("Get orderId from cache with closeOrderId({})", execution.getOrderId());

        // 成行注文で決済注文した場合はorderIdのキャッシュが間に合わないのでsleepする
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {}

        final Long orderId = orderCachePort.getOriginalOrderId(execution.getOrderId())
            .orElseThrow(() -> new IllegalStateException("Cannot find root order id from cache"));
        orderCachePort.removeMapping(execution.getOrderId());

        // DB上のオーダー情報を更新
        final Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("Cannot find order from DB"));
        order.close(LocalDateTime.now(), new Price(execution.getPrice()));
        orderRepository.update(order);
        log.info("Order updated: {}", order);
    }
}
