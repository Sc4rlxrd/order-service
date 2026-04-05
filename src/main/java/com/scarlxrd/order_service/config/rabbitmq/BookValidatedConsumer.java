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

        if(order.getStatus() != OrderStatus.CREATED ){
            log.warn("Order already processed: {} ", order.getId());
            return;
        }

        if(!event.isAvailable()){
            order.setStatus(OrderStatus.CANCELLED);
            repository.save(order);
            return;
        }


        if (event.getPrice() == null){
            log.error("Price is null for event: {}", event);
            return;
        }
        OrderItem item = new OrderItem();
        item.setBookId(event.getBookId());
        item.setQuantity(event.getQuantity());
        item.setPrice(event.getPrice());

        order.addItem(item);

        BigDecimal total = order.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if(total.compareTo(BigDecimal.ZERO) <= 0){
            log.warn("Invalid total for order: {}", order.getId());
            return;
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.VALIDATED);

        repository.save(order);

        log.info("Order {} validated with total {} ", order.getId(),total);

        PaymentRequestDTO payment = new PaymentRequestDTO();
        payment.setOrderId(order.getId());
        payment.setAmount(total);

        log.info("Sending payment request: {}",payment);

        rabbitTemplate.convertAndSend(
                "book.events",
                "payment.process",
                payment
        );

    }
}
