package com.scarlxrd.order_service.service;

import com.scarlxrd.order_service.config.rabbitmq.OrderPublisher;
import com.scarlxrd.order_service.dto.*;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.mapper.OrderMapper;
import com.scarlxrd.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final OrderPublisher orderPublisher;


    @Transactional
    public OrderResponseDTO create(CreateOrderDTO dto, String email) {

        Order order = mapper.toEntity(dto);
        order.setClientId(dto.getClientId());
        var total = BigDecimal.ZERO;
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);


        if (order.getItems() != null) {
            order.getItems().forEach(item -> item.setOrder(order));
        }

        Order savedOrder = repository.save(order);

        OrderCreatedEvent orderEvent = new OrderCreatedEvent();
        orderEvent.setOrderId(savedOrder.getId());
        orderEvent.setCustomerEmail(email);

        orderPublisher.publishOrderCreated(orderEvent);

        dto.getItems().forEach(item -> {

            BookValidationRequest event = new BookValidationRequest(
                    savedOrder.getId(),
                    item.getBookId(),
                    item.getQuantity()
            );

            rabbitTemplate.convertAndSend(
                    "book.events",
                    "book.validate",
                    event
            );
        });

        return mapper.toResponse(savedOrder);
    }
}
