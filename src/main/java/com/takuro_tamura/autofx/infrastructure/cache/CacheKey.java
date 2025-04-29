package com.takuro_tamura.autofx.infrastructure.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CacheKey {
    CLOSE_ORDER_ID("closeOrderId.%s"),
    WS_AUTH_TOKEN("wsAuthToken"),
    TRADE_ENABLED("tradeEnabled"),
    ;

    @Getter
    private final String key;

    public String build(String value) {
        return String.format(this.key, value);
    }
}
