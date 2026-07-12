package com.takuro_tamura.autofx.application.state;

import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * トレード状態確認用サービス
 * キャッシュ経由で現在のトレード有効化状態を確認
 */
@Service
@RequiredArgsConstructor
public class TradeStatePortal {

    private final RedisCacheService redisCacheService;

    /**
     * 現在のトレード有効化状態を確認
     * @return true: トレード有効, false: トレード無効
     */
    public boolean isTradingEnabled() {
        return redisCacheService.<Boolean>get(CacheKey.TRADE_ENABLED.getKey())
            .orElse(Boolean.TRUE);
    }
}
