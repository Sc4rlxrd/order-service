package com.scarlxrd.order_service.entity;

public enum OrderStatus {
    CREATED,
    PENDING,
    VALIDATING,
    VALIDATED,
    PAID,
    CANCELLED
}
