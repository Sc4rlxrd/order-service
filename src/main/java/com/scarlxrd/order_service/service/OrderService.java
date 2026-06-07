package com.scarlxrd.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scarlxrd.order_service.config.metrics.OutboxMetrics;
import com.scarlxrd.order_service.dto.*;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import com.scarlxrd.order_service.entity.OrderStatus;
import com.scarlxrd.order_service.exception.BusinessException;
import com.scarlxrd.order_service.mapper.OrderMapper;
import com.scarlxrd.order_service.outbox.OutboxEvent;
import com.scarlxrd.order_service.outbox.OutboxRepository;
import com.scarlxrd.order_service.outbox.OutboxStatus;
import com.scarlxrd.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OutboxMetrics outboxMetrics;

    @Transactional
    public OrderResponseDTO create(CreateOrderDTO dto, String email, String userId) {

        if (userId == null || userId.isBlank()) {
            throw new BusinessException("Authenticated user not found");
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("The order must contain at least one item.");
        }

        dto.getItems().forEach(item -> {
            if (item.getBookId() == null) {
                throw new BusinessException("BookId is mandatory");
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException("The quantity must be greater than zero.");
            }
        });

        Order order = mapper.toEntity(dto);

        UUID clientId;

        try {
            clientId = UUID.fromString(userId);
        } catch (
                IllegalArgumentException e) {
            throw new BusinessException("Invalid authenticated user");
        }

        order.setClientId(clientId);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);

        order.getItems().clear();

        dto.getItems().forEach(dtoItem -> {

            OrderItem item = new OrderItem();

            item.setBookId(dtoItem.getBookId());
            item.setQuantity(dtoItem.getQuantity());

            item.setPrice(BigDecimal.ZERO);

            item.setValidated(false);

            item.setOrder(order);

            order.getItems().add(item);
        });

        order.setTotalItems(order.getItems().size());

        Order savedOrder = repository.saveAndFlush(order);

        log.info(
                "Order created {}",
                savedOrder.getId()
        );

        saveOrderCreatedOutboxEvent(savedOrder, email);

        dto.getItems().forEach(item ->
                saveBookValidationOutboxEvent(savedOrder, item)
        );

        return mapper.toResponse(savedOrder);
    }

    private void saveOrderCreatedOutboxEvent(Order savedOrder, String email) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent();
            event.setOrderId(savedOrder.getId());
            event.setCustomerEmail(email);

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateId(savedOrder.getId())
                    .aggregateType("ORDER")
                    .eventType("ORDER_CREATED")
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);
            outboxMetrics.created();

        } catch (JsonProcessingException e) {
            outboxMetrics.failed();
            throw new BusinessException("Failed to create order created outbox event");
        }
    }

    private void saveBookValidationOutboxEvent(Order savedOrder, OrderItemDTO item) {
        try {
            BookValidationRequest event = new BookValidationRequest(
                    savedOrder.getId(),
                    item.getBookId(),
                    item.getQuantity()
            );

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateId(savedOrder.getId())
                    .aggregateType("ORDER")
                    .eventType("BOOK_VALIDATE_REQUESTED")
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);
            outboxMetrics.created();

        } catch (JsonProcessingException e) {
            outboxMetrics.failed();
            throw new BusinessException("Failed to create book validation outbox event");
        }
    }


    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrders(String userId, Pageable pageable) {

        UUID clientId = UUID.fromString(userId);

        return repository.findByClientId(clientId, pageable).map(mapper::toResponse);
    }
}
