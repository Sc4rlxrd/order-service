package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.PaymentResultEvent;
import com.scarlxrd.order_service.dto.StockDecreaseEvent;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.OrderNotFoundException;
import com.scarlxrd.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentResultConsumer {

    private final OrderRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
            queues = "payment.result.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentResult(PaymentResultEvent event) {

        Order order = repository.findById(event.getOrderId())
                .orElseThrow(()-> new OrderNotFoundException("Order not found"));

        if (event.getStatus() == null) {
            log.warn("Invalid payment event: status is null, orderId={}", event.getOrderId());
            order.setStatus(OrderStatus.CANCELLED);
            return;
        }
        if ("SUCCESS".equals(event.getStatus())) {
            order.setStatus(OrderStatus.PAID);

            order.getItems().forEach(item -> {

                StockDecreaseEvent stockEvent =
                        new StockDecreaseEvent(
                                UUID.randomUUID(),
                                order.getId(),
                                item.getBookId(),
                                item.getQuantity()
                        );

                rabbitTemplate.convertAndSend(
                        "book.events",
                        "stock.decrease",
                        stockEvent
                );

                log.info("Publishing stock decrease event: {}", stockEvent);
            });
        } else {
            order.setStatus(OrderStatus.CANCELLED);
        }

        repository.save(order);
    }
}
