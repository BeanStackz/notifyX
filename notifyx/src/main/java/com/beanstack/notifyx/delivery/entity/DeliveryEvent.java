package com.beanstack.notifyx.delivery.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.Notification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "delivery_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_delivery_provider_event",
        columnNames = {"provider_message_id", "event_type"}
    ),
    indexes = @Index(name = "idx_delivery_event_notification", columnList = "notification_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "provider_message_id", nullable = false, length = 255)
    private String providerMessageId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "recipient", length = 320)
    private String recipient;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
