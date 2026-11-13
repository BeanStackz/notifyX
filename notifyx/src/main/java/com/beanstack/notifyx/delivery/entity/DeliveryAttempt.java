package com.beanstack.notifyx.delivery.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.Notification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_attempts", uniqueConstraints = @UniqueConstraint(
    name = "uk_delivery_notification_attempt",
    columnNames = {"notification_id", "attempt_number"}
))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    private Instant completedAt;
}
