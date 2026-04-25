package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.PaymentResultEvent;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.exception.OrderNotFoundException;
import com.scarlxrd.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentResultConsumer {

    private final OrderRepository repository;

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
        } else {
            order.setStatus(OrderStatus.CANCELLED);
        }

        repository.save(order);
    }
}
