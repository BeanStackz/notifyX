package com.beanstack.notifyx.delivery.service;

import java.util.Map;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;

public record DeliveryContext(
    UUID notificationId,
    UUID projectId,
    UUID attemptId,
    String eventId,
    NotificationChannel channel,
    NotificationPriority priority,
    String recipient,
    String subject,
    String content,
    Map<String, Object> metadata,
    String projectName,
    String organizationName
) {}
