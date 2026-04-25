package com.scarlxrd.order_service.exception;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
