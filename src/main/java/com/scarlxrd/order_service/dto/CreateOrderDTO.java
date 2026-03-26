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

    @NotEmpty
    private List<OrderItemDTO> items;
}
