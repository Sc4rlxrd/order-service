package com.scarlxrd.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderItemDTO {

    @NotNull
    private UUID bookId;

    @Min(1)
    private  int quantity;
}
