package com.scarlxrd.order_service.service;

import com.scarlxrd.order_service.config.rabbitmq.OrderPublisher;
import com.scarlxrd.order_service.dto.CreateOrderDTO;
import com.scarlxrd.order_service.dto.OrderCreatedEvent;
import com.scarlxrd.order_service.dto.OrderItemDTO;
import com.scarlxrd.order_service.dto.OrderResponseDTO;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.mapper.OrderMapper;
import com.scarlxrd.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderPublisher orderPublisher;

    public OrderService(OrderRepository repository, OrderMapper mapper, OrderPublisher orderPublisher) {
        this.repository = repository;
        this.mapper = mapper;

        this.orderPublisher = orderPublisher;
    }

    @Transactional
    public OrderResponseDTO create(CreateOrderDTO dto) {

        Order order = mapper.toEntity(dto);
        order.setClientId(dto.getClientId());

        BigDecimal total = BigDecimal.ZERO;

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("Order must have at least one item");
        }

        for (OrderItemDTO itemDTO : dto.getItems()) {

            BigDecimal price = getBookPrice(itemDTO.getBookId());

            OrderItem item = new OrderItem();
            item.setBookId(itemDTO.getBookId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(price);

            order.addItem(item);

            total = total.add(
                    price.multiply(BigDecimal.valueOf(itemDTO.getQuantity()))
            );
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = repository.save(order);
        savedOrder = repository.findByIdWithItems(savedOrder.getId()).orElseThrow();


        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                null
        );

        orderPublisher.publishOrderCreated(event);

        return mapper.toResponse(savedOrder);
    }

    private BigDecimal getBookPrice(UUID bookId) {
        return BigDecimal.valueOf(50.0); // mock
    }
}
