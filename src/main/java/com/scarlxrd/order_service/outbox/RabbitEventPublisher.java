package com.scarlxrd.order_service.outbox;

import com.scarlxrd.order_service.config.metrics.RabbitEventMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitEventMetrics eventMetrics;

    private static final String EXCHANGE = "book.events";

    @Override
    public void publish(OutboxEvent event) {

        String metricEventName = toMetricEventName(event);

        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());

            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    resolveRoutingKey(event),
                    payload
            );
            eventMetrics.published(metricEventName);
        } catch (Exception e) {
            eventMetrics.failed(metricEventName);
            throw new RuntimeException("Failed to publish outbox event", e);
        }
    }

    private String resolveRoutingKey(OutboxEvent event) {

        if ("ORDER_CREATED".equals(event.getEventType())) {
            return "order.created";
        }

        if ("BOOK_VALIDATE_REQUESTED".equals(event.getEventType())) {
            return "book.validate";
        }

        throw new IllegalArgumentException(
                "Routing key não configurada: " + event.getEventType()
        );
    }

    private String toMetricEventName(OutboxEvent event) {

        if ("ORDER_CREATED".equals(event.getEventType())) {
            return "order_created";
        }

        if ("BOOK_VALIDATE_REQUESTED".equals(event.getEventType())) {
            return "book_validate";
        }

        return "unknown";
    }
}