package com.scarlxrd.order_service.controller;

import com.scarlxrd.order_service.dto.CreateOrderDTO;
import com.scarlxrd.order_service.dto.OrderResponseDTO;
import com.scarlxrd.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Gerenciamento de pedidos")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Criar pedido",
            description = """
                Cria um novo pedido para o usuário autenticado.
                
                O clientId é obtido automaticamente do JWT validado pelo Gateway.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "429", description = "Muitas requisições")
    })
    public ResponseEntity<OrderResponseDTO> create(
            @RequestBody @Valid CreateOrderDTO dto,

            @Parameter(
                    description = "Email do usuário autenticado",
                    hidden = true
            )
            @RequestHeader(value = "X-User-Email", required = false)
            String email,

            @Parameter(
                    description = "ID do usuário autenticado",
                    hidden = true
            )
            @RequestHeader(value = "X-User-Id", required = false)
            String userId
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(dto, email, userId));
    }

    @GetMapping("/me")
    public Page<OrderResponseDTO> getMyOrders(

            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false)
            String userId,
            Pageable pageable
    ) {

        return service.getMyOrders(userId,pageable);
    }
}
