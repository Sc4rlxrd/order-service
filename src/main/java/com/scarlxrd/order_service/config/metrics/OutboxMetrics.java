package com.scarlxrd.order_service.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMetrics {

    private final MeterRegistry registry;

    public void created() {
        Counter.builder("outbox_events_created_total")
                .tag("service", "order-service")
                .register(registry)
                .increment();
    }

    public void published() {
        Counter.builder("outbox_events_published_total")
                .tag("service", "order-service")
                .register(registry)
                .increment();
    }

    public void failed() {
        Counter.builder("outbox_events_failed_total")
                .tag("service", "order-service")
                .register(registry)
                .increment();
    }
}
