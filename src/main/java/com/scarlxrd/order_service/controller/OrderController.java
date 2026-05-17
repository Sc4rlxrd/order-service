package com.scarlxrd.order_service.controller;

import com.scarlxrd.order_service.dto.CreateOrderDTO;
import com.scarlxrd.order_service.dto.OrderResponseDTO;
import com.scarlxrd.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Gerenciamento de pedidos")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(
            summary = "Criar pedido",
            description = "Cria um novo pedido e inicia o fluxo de validação de livros e pagamento"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou itens ausentes"),
            @ApiResponse(responseCode = "429", description = "Muitas requisições")
    })
    @PostMapping
    public ResponseEntity< OrderResponseDTO >create(@RequestBody @Valid CreateOrderDTO dto,
                                                    @Parameter(description = "Email do usuário injetado pelo gateway", hidden = true)
                                                    @RequestHeader(value = "X-User-Email", required = false) String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto, email));
    }
}
