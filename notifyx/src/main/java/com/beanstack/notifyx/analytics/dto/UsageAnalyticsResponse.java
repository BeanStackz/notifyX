package com.beanstack.notifyx.analytics.dto;

import java.util.Map;
import java.util.List;

public record UsageAnalyticsResponse(
    long totalNotifications,
    long projectCount,
    Map<String, Long> byStatus,
    Map<String, Long> byChannel,
    List<DailyNotificationPoint> dailySeries,
    long deliveredNotifications,
    long failedNotifications,
    double deliveryRate
) {}
