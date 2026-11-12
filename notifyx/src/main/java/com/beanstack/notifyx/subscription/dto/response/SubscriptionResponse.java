package com.beanstack.notifyx.subscription.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.subscription.entity.SubscriptionStatus;

public record SubscriptionResponse(
    UUID id,
    UUID organizationId,
    String planCode,
    String planName,
    SubscriptionStatus status,
    long monthlyQuota,
    long notificationsUsed,
    long remainingNotifications,
    Instant currentPeriodStart,
    Instant currentPeriodEnd
) {}