package com.beanstack.notifyx.outbox.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.outbox.entity.OutboxEvent;

public interface OutboxEventRepository
    extends JpaRepository<OutboxEvent, UUID> {

        @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE (
            status = 'PENDING'
            AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            AND (
                aggregate_id NOT IN (SELECT id FROM notifications)
                OR EXISTS (
                    SELECT 1 FROM notifications n
                    WHERE n.id = aggregate_id
                    AND (n.scheduled_at IS NULL OR n.scheduled_at <= :now)
                )
            )
        )
        OR (
            status = 'PROCESSING'
            AND locked_at IS NOT NULL
            AND locked_at <= :staleBefore
        )
        ORDER BY created_at
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
        List<OutboxEvent> claimPendingEvents(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
        );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        update OutboxEvent event
        set event.status = :status,
            event.lockedAt = null,
            event.lockedBy = null,
            event.lastError = :lastError,
            event.nextAttemptAt = :nextAttemptAt,
            event.publishedAt = :publishedAt
        where event.id = :id
          and event.status = com.beanstack.notifyx.outbox.entity.OutboxStatus.PROCESSING
          and event.lockedBy = :publisherId
        """)
    int updatePublicationState(
        @Param("id") UUID id,
        @Param("publisherId") String publisherId,
        @Param("status") com.beanstack.notifyx.outbox.entity.OutboxStatus status,
        @Param("lastError") String lastError,
        @Param("nextAttemptAt") Instant nextAttemptAt,
        @Param("publishedAt") Instant publishedAt
    );
}