package com.scarlxrd.order_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Dados de resposta de um pedido")
public class OrderResponseDTO {

    @Schema(description = "ID do pedido", example = "7f684100-9e25-48ee-871a-1dd41262ca31")
    private UUID id;

    @Schema(description = "ID do cliente", example = "14fa25ed-f141-4502-927e-e5402b934066")
    private UUID clientId;

    @Schema(description = "Valor total do pedido", example = "360.00")
    private BigDecimal totalAmount;

    @Schema(description = "Status do pedido", example = "PENDING",
            allowableValues = {"CREATED", "PENDING", "VALIDATED", "PAID", "CANCELLED"})
    private String status;

    @Schema(description = "Data de criação", example = "2026-05-17T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Itens do pedido")
    private List<OrderItemResponseDTO> items;
}
