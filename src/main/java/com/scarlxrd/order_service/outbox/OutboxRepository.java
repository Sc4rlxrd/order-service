package com.scarlxrd.order_service.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status IN :statuses
              AND e.retryCount < :maxRetries
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPendingOrFailedForUpdate(
            @Param("statuses") List<OutboxStatus> statuses,
            @Param("maxRetries") int maxRetries,
            Pageable pageable
    );
}
