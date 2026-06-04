package com.scarlxrd.order_service.outbox;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {
            try {
                event.markAsProcessing();

                eventPublisher.publish(event);

                event.markAsProcessed();

                log.info("Outbox event published successfully. eventId={}, eventType={}",
                        event.getEventId(), event.getEventType());

            } catch (Exception ex) {
                event.markAsFailed(ex.getMessage());

                log.error("Failed to publish outbox event. eventId={}, eventType={}",
                        event.getEventId(), event.getEventType(), ex);
            }
        }
    }
}