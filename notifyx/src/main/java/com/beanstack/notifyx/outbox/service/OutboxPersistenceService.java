package com.beanstack.notifyx.outbox.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.outbox.entity.OutboxEvent;
import com.beanstack.notifyx.outbox.entity.OutboxStatus;
import com.beanstack.notifyx.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxPersistenceService {

    private final OutboxEventRepository outboxRepository;

    @Value("${notifyx.outbox.batch-size:50}")
    private int batchSize;

    @Value("${notifyx.outbox.lock-timeout-seconds:120}")
    private long lockTimeoutSeconds;

    @Value("${notifyx.outbox.max-attempts:10}")
    private int maxAttempts;

    @Value("${notifyx.outbox.initial-retry-delay-seconds:2}")
    private long initialRetryDelaySeconds;

    @Value("${notifyx.outbox.max-retry-delay-seconds:300}")
    private long maxRetryDelaySeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimEvents(String publisherId) {
        Instant now = Instant.now();
        List<OutboxEvent> events = outboxRepository.claimPendingEvents(
            now,
            now.minusSeconds(lockTimeoutSeconds),
            batchSize
        );

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setLockedAt(now);
            event.setLockedBy(publisherId);
            event.setLastError(null);
        }

        return outboxRepository.saveAll(events);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId, String publisherId) {
        OutboxEvent event = findEvent(eventId);
        if (!publisherId.equals(event.getLockedBy()) ||
            event.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLockedAt(null);
        event.setLockedBy(null);
        event.setNextAttemptAt(null);
        event.setLastError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, String publisherId, Exception exception) {
        OutboxEvent event = findEvent(eventId);
        if (!publisherId.equals(event.getLockedBy()) ||
            event.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLockedAt(null);
        event.setLockedBy(null);
        event.setLastError(message(exception));

        if (attempts >= maxAttempts) {
            event.setStatus(OutboxStatus.FAILED);
            event.setNextAttemptAt(null);
        } else {
            event.setStatus(OutboxStatus.PENDING);
            event.setNextAttemptAt(
                Instant.now().plusSeconds(retryDelaySeconds(attempts))
            );
        }
    }

    private OutboxEvent findEvent(UUID eventId) {
        return outboxRepository.findById(eventId)
            .orElseThrow(() ->
                new IllegalStateException("Outbox event disappeared")
            );
    }

    private long retryDelaySeconds(int attempts) {
        long multiplier = 1L << Math.min(attempts - 1, 30);
        return Math.min(
            maxRetryDelaySeconds,
            Math.max(initialRetryDelaySeconds, initialRetryDelaySeconds * multiplier)
        );
    }

    private String message(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
            ? exception.getClass().getSimpleName()
            : message;
    }
}
