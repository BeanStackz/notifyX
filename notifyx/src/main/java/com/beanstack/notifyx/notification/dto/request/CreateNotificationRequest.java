package com.beanstack.notifyx.notification.dto.request;

import java.time.Instant;
import java.util.Map;

import com.beanstack.notifyx.notification.entity.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(

    @NotNull(message = "Notification channel is required")
    NotificationChannel channel,

    @NotBlank(message = "Recipient is required")
    @Size(max = 320, message = "Recipient cannot exceed 320 characters")
    String recipient,

    @Size(max = 255, message = "Subject cannot exceed 255 characters")
    String subject,

    @NotBlank(message = "Notification content is required")
    @Size(max = 10000, message = "Content cannot exceed 10000 characters")
    String content,

    Map<String, Object> metadata,

    Instant scheduledAt

) {}