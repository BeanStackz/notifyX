package com.beanstack.notifyx.outbox.dto;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;

public record NotificationAcceptedEvent(
    UUID notificationId,
    UUID projectId,
    NotificationChannel channel,
    NotificationPriority priority,
    Instant scheduledAt,
    Instant createdAt
) {}