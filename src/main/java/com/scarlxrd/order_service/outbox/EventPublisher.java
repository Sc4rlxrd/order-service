package com.scarlxrd.order_service.outbox;

public interface EventPublisher {
    void publish(OutboxEvent event);
}