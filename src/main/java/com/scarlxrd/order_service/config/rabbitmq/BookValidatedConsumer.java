package com.scarlxrd.order_service.config.rabbitmq;

import com.scarlxrd.order_service.dto.BookValidatedEvent;
import com.scarlxrd.order_service.dto.PaymentRequestDTO;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
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
    public void handle(BookValidatedEvent event){

        log.info("Event received: {}",event);

        Order order = repository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderId()));

        if(order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order already processed: {} ", order.getId());
            return;
        }
        if(!event.isAvailable()){
            order.setStatus(OrderStatus.CANCELLED);
            repository.save(order);
            return;
        }


        if (event.getPrice() == null){
           throw new BusinessException("Price is null for bookId: " + event.getBookId());
        }

        boolean alreadyProcessed = order.getItems().stream().anyMatch(item -> item.getBookId().equals(event.getBookId()));

        if (alreadyProcessed) {
            log.warn("Duplicate event for bookId: {} ", event.getBookId());
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
            throw new BusinessException("Invalid total for order: " + order.getId());
        }

        order.setTotalAmount(total);
        boolean allItemsValidated = order.getItems().size() == order.getTotalItems();

        if (allItemsValidated) {
            order.setStatus(OrderStatus.VALIDATED);
            repository.save(order);

            log.info("Order {} fully validated with total {}", order.getId(), total);

            PaymentRequestDTO payment = new PaymentRequestDTO();
            payment.setOrderId(order.getId());
            payment.setAmount(total);

            log.info("Sending payment request: {}", payment);

            rabbitTemplate.convertAndSend(
                    "book.events",
                    "payment.process",
                    payment
            );
        } else {
            repository.save(order);
            log.info("Order {} partially validated {}/{} items",
                    order.getId(),
                    order.getItems().size(),
                    order.getTotalItems()
            );
        }
          log.info("Order {} fully validated with total {}", order.getId(), total);

    }
}
