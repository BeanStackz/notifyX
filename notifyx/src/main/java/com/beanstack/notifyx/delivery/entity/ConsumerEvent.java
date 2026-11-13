package com.beanstack.notifyx.delivery.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.Notification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "notification_consumer_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_consumer_event",
        columnNames = "event_id"
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsumerEventStatus status;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    private Instant completedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
