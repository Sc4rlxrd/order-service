package com.scarlxrd.order_service.exception;

public class OrderItemNotFoundException extends BusinessException {
    public OrderItemNotFoundException(String message) {
        super(message);
    }
}
