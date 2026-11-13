package com.beanstack.notifyx.delivery.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.delivery.entity.DeliveryEvent;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID> {
    boolean existsByProviderMessageIdAndEventType(String providerMessageId, String eventType);
    List<DeliveryEvent> findAllByNotification_IdOrderByOccurredAtAsc(UUID notificationId);

    @Modifying
    @Query(value = """
        INSERT INTO delivery_events (
            id, notification_id, provider_message_id, event_type,
            recipient, occurred_at, received_at
        ) VALUES (
            :id, :notificationId, :messageId, :eventType,
            :recipient, :occurredAt, :receivedAt
        )
        ON CONFLICT (provider_message_id, event_type) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("id") UUID id,
        @Param("notificationId") UUID notificationId,
        @Param("messageId") String messageId,
        @Param("eventType") String eventType,
        @Param("recipient") String recipient,
        @Param("occurredAt") java.time.Instant occurredAt,
        @Param("receivedAt") java.time.Instant receivedAt
    );
}
