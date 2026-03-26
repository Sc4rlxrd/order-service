package com.scarlxrd.order_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class OrderItemResponseDTO {

    private UUID bookId;
    private int quantity;
    private BigDecimal price;
}
