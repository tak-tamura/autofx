package com.takuro_tamura.autofx.infrastructure.external.exception;

import com.takuro_tamura.autofx.infrastructure.external.response.ApiErrorCode;
import lombok.Getter;

public class ApiErrorException extends RuntimeException {
    @Getter
    private ApiErrorCode errorCode;

    public ApiErrorException(String message) {
        super(message);
    }

    public ApiErrorException(ApiErrorCode errorCode, String message) {
        this(message);
        this.errorCode = errorCode;
    }

    public ApiErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiErrorException(ApiErrorCode errorCode, String message, Throwable cause) {
        this(message, cause);
        this.errorCode = errorCode;
    }
}
