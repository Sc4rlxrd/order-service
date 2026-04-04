package com.scarlxrd.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookValidatedEvent {

    private UUID orderId;
    private String isbn;
    private UUID bookId;
    private BigDecimal price;
    private boolean available;
    private int quantity;

}
