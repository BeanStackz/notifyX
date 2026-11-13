package com.beanstack.notifyx.analytics.dto;

import java.util.Map;

public record UsageAnalyticsResponse(
    long totalNotifications,
    long projectCount,
    Map<String, Long> byStatus,
    Map<String, Long> byChannel
) {}
