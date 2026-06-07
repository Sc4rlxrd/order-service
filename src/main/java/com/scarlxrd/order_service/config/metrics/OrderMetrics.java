package com.scarlxrd.order_service.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    public void paid(){
        Counter.builder("orders_paid_total")
                .description("Total paid orders")
                .tag("service", "order-service")
                .register(this.meterRegistry)
                .increment();
    }

    public void validated() {
        Counter.builder("orders_validated_total")
                .description("Total de pedidos validados")
                .tag("service", "order-service")
                .register(this.meterRegistry)
                .increment();
    }

    public void cancelled(String reason) {
        Counter.builder("orders_cancelled_total")
                .description("Total de pedidos cancelados")
                .tag("service", "order-service")
                .tag("reason", reason)
                .register(this.meterRegistry)
                .increment();
    }

}
