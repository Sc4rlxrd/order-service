package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.BookValidatedEvent;
import com.scarlxrd.order_service.dto.PaymentRequestDTO;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import com.scarlxrd.order_service.entity.OrderStatus;
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

    @RabbitListener(queues = "book.validated.queue")
    @Transactional
    public void handle(BookValidatedEvent event){

        log.info("Event received: {}",event);


        Order order = repository.findById(event.getOrderId())
                .orElse(null);

        if (order == null) {
            log.warn("Order not found for id: {}", event.getOrderId());
            return;
        }

        if(!event.isAvailable()){
            order.setStatus(OrderStatus.CANCELLED);
            repository.save(order);
            return;
        }

        OrderItem item = new OrderItem();
        item.setBookId(event.getBookId());
        item.setQuantity(event.getQuantity());
        item.setPrice(event.getPrice());

        order.addItem(item);

        BigDecimal total = order.getItems().stream().filter(i -> i.getPrice() != null).map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        repository.save(order);

        PaymentRequestDTO payment = new PaymentRequestDTO();
        payment.setOrderId(order.getId());
        payment.setAmount(total);

        rabbitTemplate.convertAndSend(
                "book.events",
                "payment.process",
                payment
        );

    }
}
