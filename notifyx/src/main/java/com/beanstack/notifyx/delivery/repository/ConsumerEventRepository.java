package com.beanstack.notifyx.delivery.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.delivery.entity.ConsumerEvent;

public interface ConsumerEventRepository
    extends JpaRepository<ConsumerEvent, UUID> {
    Optional<ConsumerEvent> findByEventId(String eventId);

    @Modifying
    @Query(value = """
        INSERT INTO notification_consumer_events
            (id, event_id, notification_id, status, claimed_at)
        VALUES
            (:id, :eventId, :notificationId, 'PROCESSING', :claimedAt)
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("id") UUID id,
        @Param("eventId") String eventId,
        @Param("notificationId") UUID notificationId,
        @Param("claimedAt") java.time.Instant claimedAt
    );
}
