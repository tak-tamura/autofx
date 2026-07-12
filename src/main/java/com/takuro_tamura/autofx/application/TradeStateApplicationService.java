package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.OrderStatus;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * トレード状態管理サービス
 * トレード有効化/無効化とそれに伴う処理を実行
 * スケジューリングは TradeScheduler で処理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeStateApplicationService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RedisCacheService redisCacheService;
    private final TradeConfigParameterService tradeConfigParameterService;

    /**
     * トレード停止処理
     * - キャッシュにトレード無効フラグを設定
     * - 開いているポジションがあれば決済注文を発行
     */
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

    /**
     * トレード再開処理
     * - キャッシュにトレード有効フラグを設定
     */
    public void resumeTrade() {
        redisCacheService.save(CacheKey.TRADE_ENABLED.getKey(), Boolean.TRUE);
    }
}
