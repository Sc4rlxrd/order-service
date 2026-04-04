package com.scarlxrd.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookValidationRequest {

    private UUID orderId;
    private UUID bookId;
    private int quantity;
}
