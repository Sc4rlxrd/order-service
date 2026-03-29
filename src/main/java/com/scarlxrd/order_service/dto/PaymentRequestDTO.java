package com.scarlxrd.order_service.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequestDTO {

    private UUID orderId;

    private BigDecimal amount;
}
