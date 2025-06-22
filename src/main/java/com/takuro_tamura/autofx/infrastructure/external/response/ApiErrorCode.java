package com.takuro_tamura.autofx.infrastructure.external.response;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public enum ApiErrorCode {
    ERR_143("ERR-143", "Restricted"),
    ERR_5003("ERR-5003", "Exceeds rate limit"),
    ERR_5106("ERR-5106", "Invalid parameter"),
    ERR_5201("ERR-5202", "Under maintenance"),
    ;

    private final String code;

    private final String description;

    public static ApiErrorCode fromCode(String code) {
        return Arrays.stream(ApiErrorCode.values())
            .filter(errorCode -> errorCode.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No such code: " + code));
    }
}
