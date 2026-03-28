package com.scarlxrd.order_service.service;

import com.scarlxrd.order_service.dto.CreateOrderDTO;
import com.scarlxrd.order_service.dto.OrderItemDTO;
import com.scarlxrd.order_service.dto.OrderResponseDTO;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
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

    public OrderService(OrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }


    @Transactional
    public OrderResponseDTO create(CreateOrderDTO dto) {

        Order order = mapper.toEntity(dto);
        order.setClientId(dto.getClientId());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemDTO itemDTO : dto.getItems()) {

            BigDecimal price = getBookPrice(itemDTO.getBookId());

            OrderItem item = new OrderItem();
            item.setBookId(itemDTO.getBookId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(price);

            order.addItem(item);

            total = total.add(price.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        order.setTotalAmount(total);

        Order saved = repository.save(order);

        return mapper.toResponse(saved);
    }

    private BigDecimal getBookPrice(UUID bookId) {
        return BigDecimal.valueOf(50.0); // mock
    }
}
