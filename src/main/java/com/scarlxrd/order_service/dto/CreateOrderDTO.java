package com.scarlxrd.order_service.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderDTO {

    @NotNull
    private UUID clientId;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemDTO> items;
}
