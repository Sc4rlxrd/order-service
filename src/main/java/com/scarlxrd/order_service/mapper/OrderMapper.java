package com.scarlxrd.order_service.mapper;


import com.scarlxrd.order_service.dto.CreateOrderDTO;
import com.scarlxrd.order_service.dto.OrderItemResponseDTO;
import com.scarlxrd.order_service.dto.OrderResponseDTO;
import com.scarlxrd.order_service.entity.Order;
import com.scarlxrd.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface  OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(CreateOrderDTO dto);

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponseDTO toResponse(Order order);

    OrderItemResponseDTO toItemResponse(OrderItem item);
}
