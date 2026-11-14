package com.beanstack.notifyx.delivery.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.delivery.entity.ConsumerEvent;
import com.beanstack.notifyx.delivery.entity.ConsumerEventStatus;
import com.beanstack.notifyx.delivery.repository.ConsumerEventRepository;
import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsumerIdempotencyService {
    private final ConsumerEventRepository eventRepository;
    private final NotificationRepository notificationRepository;

    @Value("${notifyx.consumer.lock-timeout-seconds:300}")
    private long lockTimeoutSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claim(String eventId, UUID notificationId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("RabbitMQ event ID is missing");
        }

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Notification referenced by event was not found"
            ));

        Instant now = Instant.now();
        ConsumerEvent event = eventRepository.findByEventId(eventId).orElse(null);

        if (event == null) {
            int inserted = eventRepository.insertIfAbsent(
                UUID.randomUUID(),
                eventId,
                notificationId,
                now
            );
            if (inserted == 1) {
                return ClaimResult.CLAIMED;
            }

            event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException(
                    "Consumer event receipt was not created"
                ));
        }

        if (event.getStatus() == ConsumerEventStatus.COMPLETED) {
            return ClaimResult.ALREADY_COMPLETED;
        }

        if (event.getStatus() == ConsumerEventStatus.PROCESSING &&
            event.getClaimedAt().plusSeconds(lockTimeoutSeconds).isAfter(now)) {
            return ClaimResult.IN_PROGRESS;
        }

        event.setStatus(ConsumerEventStatus.PROCESSING);
        event.setClaimedAt(now);
        event.setLastError(null);
        return ClaimResult.CLAIMED;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String eventId) {
        ConsumerEvent event = find(eventId);
        event.setStatus(ConsumerEventStatus.COMPLETED);
        event.setCompletedAt(Instant.now());
        event.setLastError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String eventId, Exception exception) {
        ConsumerEvent event = find(eventId);
        event.setStatus(ConsumerEventStatus.FAILED);
        event.setCompletedAt(Instant.now());
        event.setLastError(errorMessage(exception));
    }

    private ConsumerEvent find(String eventId) {
        return eventRepository.findByEventId(eventId)
            .orElseThrow(() -> new IllegalStateException(
                "Consumer event receipt was not found"
            ));
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null
            ? exception.getClass().getSimpleName()
            : exception.getMessage();
    }

    public enum ClaimResult {
        CLAIMED,
        ALREADY_COMPLETED,
        IN_PROGRESS
    }
}
