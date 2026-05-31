package com.scarlxrd.order_service.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Dados para criação de um novo pedido")
public class CreateOrderDTO {

    @Schema(description = "Lista de itens do pedido")
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemDTO> items;
}
