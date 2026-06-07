package com.scarlxrd.order_service.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitEventMetrics {

    private final MeterRegistry registry;

    public void consumed(String event) {
        Counter.builder("rabbitmq_events_consumed_total")
                .description("Total de eventos consumidos do RabbitMQ")
                .tag("service", "order-service")
                .tag("event", event)
                .register(registry)
                .increment();
    }

    public void published(String event) {
        Counter.builder("rabbitmq_events_published_total")
                .description("Total de eventos publicados no RabbitMQ")
                .tag("service", "order-service")
                .tag("event", event)
                .register(registry)
                .increment();
    }

    public void failed(String event) {
        Counter.builder("rabbitmq_events_failed_total")
                .description("Total de falhas em eventos RabbitMQ")
                .tag("service", "order-service")
                .tag("event", event)
                .register(registry)
                .increment();
    }

    public void duplicated(String event) {
        Counter.builder("rabbitmq_events_duplicated_total")
                .description("Total de eventos duplicados do RabbitMQ")
                .tag("service", "order-service")
                .tag("event", event)
                .register(registry)
                .increment();
    }
}
