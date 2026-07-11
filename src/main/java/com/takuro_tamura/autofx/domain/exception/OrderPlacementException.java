package com.takuro_tamura.autofx.domain.exception;

/**
 * 注文ブローカーへの注文送信に失敗した場合に発生する例外
 */
public class OrderPlacementException extends RuntimeException {
    
    public OrderPlacementException(String message) {
        super(message);
    }
    
    public OrderPlacementException(String message, Throwable cause) {
        super(message, cause);
    }
}
