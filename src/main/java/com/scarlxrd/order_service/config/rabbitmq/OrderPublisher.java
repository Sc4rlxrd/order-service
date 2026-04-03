package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final String  EXCHANGE = "book.events";
    private static final String ROUTING_KEY = "order.created";

    public void publishOrderCreated(OrderCreatedEvent event) {

        log.info("Publishing order.created event for orderId={}", event.getOrderId());

        rabbitTemplate.convertAndSend(
                EXCHANGE,
                ROUTING_KEY,
                event
        );
    }
}
