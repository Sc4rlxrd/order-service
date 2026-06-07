package com.scarlxrd.order_service.config.rabbitmq;


import com.scarlxrd.order_service.config.metrics.RabbitEventMetrics;
import com.scarlxrd.order_service.dto.OrderCreatedEvent;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final OrderRepository repository;
    private final RabbitEventMetrics rabbitMetrics;

    @RabbitListener(
            queues = "order.created.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void handle(OrderCreatedEvent event) {

        rabbitMetrics.consumed("order_created");

        log.info(
                "Received order.created {}",
                event.getOrderId()
        );

        Order order = repository.findById(
                event.getOrderId()
        ).orElseThrow();

        order.getItems().forEach(item -> {
            log.info(
                    "Livro={} qtd={}",
                    item.getBookId(),
                    item.getQuantity()
            );
        });

    }
}