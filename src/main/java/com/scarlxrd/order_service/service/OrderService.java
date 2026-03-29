package com.scarlxrd.order_service.service;

import com.scarlxrd.order_service.config.client.PaymentClient;
import com.scarlxrd.order_service.dto.*;
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
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository repository, OrderMapper mapper, PaymentClient paymentClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.paymentClient = paymentClient;
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

        Order savedOrder = repository.save(order);

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
        paymentRequest.setOrderId(savedOrder.getId());
        paymentRequest.setAmount(total);

        PaymentResponseDTO paymentResponse =
                paymentClient.processPayment(paymentRequest).join();

        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            savedOrder.setStatus(OrderStatus.PAID);
        } else {
            savedOrder.setStatus(OrderStatus.CANCELLED);
        }

        return mapper.toResponse(repository.save(savedOrder));
    }

    private BigDecimal getBookPrice(UUID bookId) {
        return BigDecimal.valueOf(50.0); // mock
    }
}
