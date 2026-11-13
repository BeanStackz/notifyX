package com.beanstack.notifyx.delivery.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.delivery.entity.DeliveryAttempt;
import com.beanstack.notifyx.delivery.entity.DeliveryStatus;
import com.beanstack.notifyx.delivery.provider.NotificationProvider;
import com.beanstack.notifyx.delivery.repository.DeliveryAttemptRepository;
import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.entity.NotificationStatus;
import com.beanstack.notifyx.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final List<NotificationProvider> providers;
    private final ConsumerIdempotencyService idempotencyService;

    @Transactional
    public DeliveryAttempt start(UUID notificationId, String eventId) {
        ConsumerIdempotencyService.ClaimResult claim =
            idempotencyService.claim(eventId, notificationId);
        if (claim != ConsumerIdempotencyService.ClaimResult.CLAIMED) {
            return null;
        }

        Notification notification = notificationRepository
            .findWithProjectAndOrganizationById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (notification.getStatus() == NotificationStatus.SENT) {
            idempotencyService.complete(eventId);
            return null;
        }
        int attemptNumber = attemptRepository
            .findFirstByNotification_IdOrderByAttemptNumberDesc(notificationId)
            .map(value -> value.getAttemptNumber() + 1)
            .orElse(1);
        notification.setStatus(NotificationStatus.PROCESSING);
        return attemptRepository.save(DeliveryAttempt.builder()
            .notification(notification)
            .eventId(eventId)
            .attemptNumber(attemptNumber)
            .status(DeliveryStatus.STARTED)
            .startedAt(Instant.now())
            .build());
    }

    public NotificationProvider providerFor(Notification notification) {
        return providers.stream()
            .filter(provider -> provider.getChannel() == notification.getChannel())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No provider configured for " + notification.getChannel()
            ));
    }

    @Transactional
    public void complete(DeliveryAttempt attempt, String providerMessageId, String response) {
        attempt.setStatus(DeliveryStatus.SENT);
        attempt.setProviderMessageId(providerMessageId);
        attempt.setProviderResponse(response);
        attempt.setCompletedAt(Instant.now());
        attempt.getNotification().setStatus(NotificationStatus.SENT);
        attemptRepository.save(attempt);
        notificationRepository.save(attempt.getNotification());
        idempotencyService.complete(attempt.getEventId());
    }

    @Transactional
    public void fail(DeliveryAttempt attempt, Exception exception) {
        attempt.setStatus(DeliveryStatus.FAILED);
        attempt.setErrorMessage(exception.getMessage());
        attempt.setCompletedAt(Instant.now());
        attempt.getNotification().setStatus(NotificationStatus.FAILED);
        attemptRepository.save(attempt);
        notificationRepository.save(attempt.getNotification());
        idempotencyService.fail(attempt.getEventId(), exception);
    }
}
