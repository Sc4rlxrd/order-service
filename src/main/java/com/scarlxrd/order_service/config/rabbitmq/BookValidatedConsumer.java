package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.BookValidatedEvent;
import com.scarlxrd.order_service.dto.PaymentRequestDTO;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.exception.OrderItemNotFoundException;
import com.scarlxrd.order_service.exception.OrderNotFoundException;
import com.scarlxrd.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Log4j2
public class BookValidatedConsumer {

    private final OrderRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
            queues = "book.validated.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void handle(BookValidatedEvent event) {

        log.info(
                "Looking order {} from event {}",
                event.getOrderId(),
                event
        );

        Order order = repository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found: " + event.getOrderId())
                );


        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.VALIDATED) {
            log.warn("Order already finalized {}", order.getId());
            return;
        }

        if (!event.isAvailable()) {
            order.setStatus(OrderStatus.CANCELLED);

            log.warn(
                    "Book {} unavailable. Cancelling order {}",
                    event.getBookId(),
                    order.getId()
            );

            repository.save(order);
            return;
        }

        if (event.getPrice() == null) {
            log.error("Price null for event {}", event);

            order.setStatus(OrderStatus.CANCELLED);

            repository.save(order);
            return;
        }

        OrderItem item = order.getItems()
                .stream()
                .filter(i -> i.getBookId().equals(event.getBookId()))
                .findFirst()
                .orElseThrow(() ->
                        new OrderItemNotFoundException(
                                "Book not found: " + event.getBookId()
                        )
                );


        if (item.isValidated()) {
            log.warn("Duplicate event book {} order {}", event.getBookId(), order.getId());
            return;
        }

        item.setValidated(true);
        item.setPrice(event.getPrice());
        item.setQuantity(event.getQuantity());

        BigDecimal total = order.getItems()
                .stream()
                .filter(OrderItem::isValidated) // só soma itens validados
                .map(i -> i.getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid total for order: " + order.getId());
        }

        order.setTotalAmount(total);

        log.info(
                "Order {} validation state: {}",
                order.getId(),
                order.getItems()
                        .stream()
                        .map(i -> String.format(
                                "book=%s validated=%s qty=%d price=%s",
                                i.getBookId(),
                                i.isValidated(),
                                i.getQuantity(),
                                i.getPrice()
                        ))
                        .toList()
        );

        boolean allItemsValidated =
                order.getItems()
                        .stream()
                        .filter(OrderItem::isValidated)
                        .count()
                        == order.getTotalItems();

        long validatedCount = order.getItems()
                .stream()
                .filter(OrderItem::isValidated)
                .count();

        log.info("Validated items {}/{}", validatedCount, order.getTotalItems());

        log.info(
                "Order={} status={} validated={}/{}",
                order.getId(),
                order.getStatus(),
                validatedCount,
                order.getTotalItems()
        );

        if (allItemsValidated) {

            log.info("ENTERING PAYMENT FLOW order={}", order.getId());

            order.setStatus(OrderStatus.VALIDATED);
            repository.save(order);

            log.info(
                    "Order {} fully validated with total {}",
                    order.getId(),
                    total
            );

            PaymentRequestDTO payment = new PaymentRequestDTO();
            payment.setOrderId(order.getId());
            payment.setAmount(total);

            log.info(
                    "Sending payment order={} total={}",
                    order.getId(),
                    total
            );

            rabbitTemplate.convertAndSend(
                    "book.events",
                    "payment.process",
                    payment
            );

        } else {
            repository.save(order);
            log.info(
                    "Order {} partially validated {}/{} items", order.getId(), order.getItems()
                            .stream()
                            .filter(OrderItem::isValidated)
                            .count(),
                    order.getTotalItems()
            );
        }
    }
}