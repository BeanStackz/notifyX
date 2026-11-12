package com.beanstack.notifyx.payment.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.subscription.entity.RazorpayWebhookEvent;

public interface RazorpayWebhookEventRepository
    extends JpaRepository<RazorpayWebhookEvent, UUID> {

    Optional<RazorpayWebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Modifying
    @Query(value = """
        INSERT INTO razorpay_webhook_events
        (id, event_id, event_type, payload, status, received_at)
        VALUES
        (:id, :eventId, :eventType, :payload, 'RECEIVED', :receivedAt)
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("id") UUID id,
        @Param("eventId") String eventId,
        @Param("eventType") String eventType,
        @Param("payload") String payload,
        @Param("receivedAt") Instant receivedAt
    );
}