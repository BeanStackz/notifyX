package com.beanstack.notifyx.notification.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;
import com.beanstack.notifyx.notification.entity.NotificationStatus;

public record NotificationResponse(
    UUID id,
    UUID projectId,
    NotificationChannel channel,
    String recipient,
    NotificationStatus status,
    NotificationPriority priority,
    Instant scheduledAt,
    Instant createdAt
) {}