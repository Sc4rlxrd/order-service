package com.scarlxrd.order_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Dados de resposta de um item do pedido")
public class OrderItemResponseDTO {

    @Schema(description = "ID do livro", example = "f89426b9-6dd0-4351-9a57-144d3e3dc090")
    private UUID bookId;

    @Schema(description = "Quantidade", example = "2")
    private int quantity;

    @Schema(description = "Preço unitário", example = "180.00")
    private BigDecimal price;
}