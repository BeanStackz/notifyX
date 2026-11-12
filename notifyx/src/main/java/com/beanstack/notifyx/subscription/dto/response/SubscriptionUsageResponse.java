package com.beanstack.notifyx.subscription.dto.response;

import java.time.Instant;

public record SubscriptionUsageResponse(
    long notificationsUsed,
    long monthlyQuota,
    long remainingNotifications,
    Instant periodStart,
    Instant periodEnd
) {}