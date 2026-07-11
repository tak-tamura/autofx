package com.takuro_tamura.autofx.infrastructure.cache;

import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisCacheOrderAdapter implements OrderCachePort {
    private final RedisCacheService redisCacheService;
    
    @Override
    public void mapCloseOrderToOriginalOrder(Long closeOrderId, Long originalOrderId) {
        redisCacheService.save(
            CacheKey.CLOSE_ORDER_ID.build(closeOrderId.toString()),
            originalOrderId
        );
    }
    
    @Override
    public Optional<Long> getOriginalOrderId(Long closeOrderId) {
        return redisCacheService.get(
            CacheKey.CLOSE_ORDER_ID.build(closeOrderId.toString())
        );
    }
    
    @Override
    public void removeMapping(Long closeOrderId) {
        redisCacheService.delete(
            CacheKey.CLOSE_ORDER_ID.build(closeOrderId.toString())
        );
    }
}
