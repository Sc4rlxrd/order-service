package com.scarlxrd.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Item de um pedido")
public class OrderItemDTO {

    @Schema(description = "ID do livro", example = "f89426b9-6dd0-4351-9a57-144d3e3dc090")
    @NotNull
    private UUID bookId;

    @Schema(description = "Quantidade desejada", example = "2")
    @Min(1)
    private  int quantity;
}
