package com.beanstack.notifyx.authorization.dto;

public record AdminDashboardResponse(
    long userCount,
    long organizationCount,
    long projectCount,
    long notificationCount
) {}
