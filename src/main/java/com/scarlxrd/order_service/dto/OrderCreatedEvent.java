package com.scarlxrd.order_service.dto;


import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {

    private UUID orderId;
    private BigDecimal amount;
    private String customerEmail;
}
