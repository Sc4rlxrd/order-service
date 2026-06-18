package com.scarlxrd.order_service.outbox;


import com.scarlxrd.order_service.config.metrics.OutboxMetrics;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final OutboxMetrics outboxMetrics;
    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events =
                outboxRepository.findPendingOrFailedForUpdate(
                        List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                        MAX_RETRIES,
                        PageRequest.of(0, BATCH_SIZE)
                );
        for (OutboxEvent event : events) {
            try {
                event.markAsProcessing();

                eventPublisher.publish(event);

                event.markAsProcessed();

                outboxMetrics.published();

                log.info("Outbox event published successfully. eventId={}, eventType={}",
                        event.getEventId(), event.getEventType());

            } catch (Exception ex) {
                event.markAsFailed(ex.getMessage());
                outboxMetrics.failed();

                log.error("Failed to publish outbox event. eventId={}, eventType={}",
                        event.getEventId(), event.getEventType(), ex);
            }
        }
    }
}