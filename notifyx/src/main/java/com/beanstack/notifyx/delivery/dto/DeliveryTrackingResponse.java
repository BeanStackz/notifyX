package com.beanstack.notifyx.delivery.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.beanstack.notifyx.delivery.entity.DeliveryStatus;
import com.beanstack.notifyx.notification.entity.*;

public record DeliveryTrackingResponse(
    UUID notificationId,
    UUID projectId,
    NotificationChannel channel,
    String recipient,
    NotificationStatus notificationStatus,
    NotificationPriority priority,
    Instant scheduledAt,
    Instant createdAt,
    List<Attempt> attempts,
    List<Event> events
) {
    public record Attempt(
        UUID id,
        int attemptNumber,
        DeliveryStatus status,
        String providerMessageId,
        String providerResponse,
        String errorMessage,
        Instant startedAt,
        Instant completedAt
    ) {}

    public record Event(
        String type,
        String providerMessageId,
        String recipient,
        Instant occurredAt,
        Instant receivedAt
    ) {}
}
