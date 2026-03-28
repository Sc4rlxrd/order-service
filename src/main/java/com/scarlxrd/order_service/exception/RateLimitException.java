package com.scarlxrd.order_service.exception;

public class RateLimitException extends BusinessException{

    public RateLimitException(String message) {
        super(message);
    }
}
