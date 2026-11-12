package com.beanstack.notifyx.notification.service;

import java.util.UUID;

import com.beanstack.notifyx.notification.dto.request.CreateNotificationRequest;
import com.beanstack.notifyx.notification.dto.response.NotificationResponse;

public interface NotificationService {

    NotificationResponse createNotification(
        UUID projectId,
        String idempotencyKey,
        CreateNotificationRequest request
    );
}