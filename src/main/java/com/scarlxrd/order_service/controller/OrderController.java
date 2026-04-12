package com.scarlxrd.order_service.controller;

import com.scarlxrd.order_service.dto.CreateOrderDTO;
import com.scarlxrd.order_service.dto.OrderResponseDTO;
import com.scarlxrd.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity< OrderResponseDTO >create(@RequestBody @Valid CreateOrderDTO dto, @RequestHeader(value = "X-User-Email", required = false) String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto, email));
    }
}
