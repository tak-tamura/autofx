package com.takuro_tamura.autofx.infrastructure.websocket;

import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RenewTokenScheduler {
    private final Logger log = LoggerFactory.getLogger(RenewTokenScheduler.class);
    private final PrivateApi privateApi;
    private final RedisCacheService redisCacheService;

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void renewToken() {
        final Optional<String> token = redisCacheService.get(CacheKey.WS_AUTH_TOKEN.getKey());
        if (token.isPresent()) {
            try {
                privateApi.extendWsToken(token.get());
                log.info("Token successfully renewed");
            } catch (Exception e) {
                log.error("Failed to renew token", e);
            }

        } else {
            log.info("Token not found in cache, skip renewing");
        }
    }
}
