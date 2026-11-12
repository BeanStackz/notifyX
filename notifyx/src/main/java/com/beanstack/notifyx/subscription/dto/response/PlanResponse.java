package com.beanstack.notifyx.subscription.dto.response;

import java.util.Set;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;

public record PlanResponse(
    UUID id,
    String code,
    String name,
    String description,
    long monthlyPriceInPaise,
    String currency,
    long monthlyQuota,
    int rateLimitPerMinute,
    NotificationPriority priority,
    Set<NotificationChannel> allowedChannels
) {}