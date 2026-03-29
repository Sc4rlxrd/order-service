package com.scarlxrd.order_service.config.client;

import com.scarlxrd.order_service.dto.PaymentRequestDTO;
import com.scarlxrd.order_service.dto.PaymentResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${payment.url}")
    private String paymentUrl;

    @Retry(name = "paymentRetry")
    @CircuitBreaker(name = "paymentCircuit", fallbackMethod = "fallback")
    @TimeLimiter(name = "paymentTimeout")
    public CompletableFuture<PaymentResponseDTO> processPayment(PaymentRequestDTO request) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Calling Payment Service...");
            return restTemplate.postForObject(
                    paymentUrl,
                    request,
                    PaymentResponseDTO.class
            );
        });
    }

    public CompletableFuture<PaymentResponseDTO> fallback(
            PaymentRequestDTO request,
            Throwable ex
    ) {
        log.error("Payment fallback triggered: {}", ex.getMessage());

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setStatus("FAILED");

        return CompletableFuture.completedFuture(response);
    }
}