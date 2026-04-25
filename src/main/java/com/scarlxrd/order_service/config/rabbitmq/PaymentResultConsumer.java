package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.PaymentResultEvent;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentResultConsumer {

    private final OrderRepository repository;

    @RabbitListener(
            queues = "payment.result.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentResult(PaymentResultEvent event) {

        Order order = repository.findById(event.getOrderId())
                .orElseThrow();

        if ("SUCCESS".equals(event.getStatus())) {
            order.setStatus(OrderStatus.PAID);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
        }

        repository.save(order);
    }
}
