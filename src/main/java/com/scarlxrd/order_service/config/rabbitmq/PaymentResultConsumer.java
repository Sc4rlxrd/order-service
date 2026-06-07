package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.config.metrics.OrderMetrics;
import com.scarlxrd.order_service.config.metrics.RabbitEventMetrics;
import com.scarlxrd.order_service.dto.PaymentResultEvent;
import com.scarlxrd.order_service.dto.StockDecreaseEvent;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.entity.ProcessedEvent;
import com.scarlxrd.order_service.exception.OrderNotFoundException;
import com.scarlxrd.order_service.repository.OrderRepository;
import com.scarlxrd.order_service.repository.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentResultConsumer {

    private final OrderRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessedEventRepository processedEvent;
    private final RabbitEventMetrics rabbitMetrics;
    private final OrderMetrics orderMetrics;

    @RabbitListener(
            queues = "payment.result.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentResult(PaymentResultEvent event) {

        rabbitMetrics.consumed("payment_result");

        log.warn(
                "PAYMENT RESULT RECEIVED order={} status={}",
                event.getOrderId(),
                event.getStatus()
        );

        Order order = repository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(()-> new OrderNotFoundException("Order not found"));

        if (event.getStatus() == null) {
            log.warn("Invalid payment event: status is null, orderId={}", event.getOrderId());
            order.setStatus(OrderStatus.CANCELLED);
            orderMetrics.cancelled("invalid_payment_event");
            repository.save(order);
            return;
        }

        if (event.getEventId() == null) {
            rabbitMetrics.failed("payment_result");
            throw new IllegalStateException(
                    "Payment event received without eventId"
            );
        }

        String eventId = event.getEventId().toString();

        if (isDuplicate(eventId)) {
            rabbitMetrics.duplicated("payment_result");
            log.warn("Duplicate payment event {}", eventId);
            return;
        }

        if ("SUCCESS".equals(event.getStatus())) {

            if (order.getStatus() == OrderStatus.PAID) {
                rabbitMetrics.duplicated("payment_result");
                log.warn(
                        "Payment already processed for order {}",
                        order.getId()
                );
                return;
            }

            order.setStatus(OrderStatus.PAID);
            orderMetrics.paid();

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
                rabbitMetrics.published("stock_decrease");
                log.info("Publishing stock decrease event: {}", stockEvent);
            });
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            orderMetrics.cancelled("payment_failed");
        }

        repository.save(order);
    }

    private boolean isDuplicate(String eventId) {
        try {
            processedEvent.save(new ProcessedEvent(eventId));
            return false;
        } catch (
                DataIntegrityViolationException e) {
            return true;
        }
    }
}
