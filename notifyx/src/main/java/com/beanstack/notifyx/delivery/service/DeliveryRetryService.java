package com.beanstack.notifyx.delivery.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.delivery.entity.ConsumerEvent;
import com.beanstack.notifyx.delivery.entity.ConsumerEventStatus;
import com.beanstack.notifyx.delivery.entity.DeliveryAttempt;
import com.beanstack.notifyx.delivery.entity.DeliveryStatus;
import com.beanstack.notifyx.delivery.repository.ConsumerEventRepository;
import com.beanstack.notifyx.delivery.repository.DeliveryAttemptRepository;
import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.entity.NotificationStatus;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
import com.beanstack.notifyx.outbox.dto.NotificationAcceptedEvent;
import com.beanstack.notifyx.outbox.entity.OutboxEvent;
import com.beanstack.notifyx.outbox.entity.OutboxStatus;
import com.beanstack.notifyx.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DeliveryRetryService {
    private final DeliveryRetryPolicy retryPolicy;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ConsumerEventRepository consumerEventRepository;
    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handleFailure(UUID attemptId, Exception exception) {
        DeliveryAttempt attempt = deliveryAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Delivery attempt not found: " + attemptId
            ));

        Notification notification = notificationRepository.findById(
            attempt.getNotification().getId()
        ).orElseThrow(() -> new IllegalArgumentException(
            "Notification not found for delivery attempt: " + attemptId
        ));

        RetryDecision decision = retryPolicy.evaluate(
            attempt.getAttemptNumber(),
            exception
        );

        attempt.setStatus(DeliveryStatus.FAILED);
        attempt.setErrorMessage(errorMessage(exception));
        attempt.setCompletedAt(Instant.now());
        deliveryAttemptRepository.save(attempt);

        consumerEventRepository.findByEventId(attempt.getEventId())
            .ifPresent(event -> {
                event.setStatus(ConsumerEventStatus.FAILED);
                event.setCompletedAt(Instant.now());
                event.setLastError(errorMessage(exception));
                consumerEventRepository.save(event);
            });

        if (decision.retryable()) {
            notification.setStatus(NotificationStatus.RETRYING);
            notificationRepository.save(notification);
            outboxEventRepository.save(OutboxEvent.builder()
                .aggregateId(notification.getId())
                .eventType("NOTIFICATION_ACCEPTED")
                .payload(toJson(new NotificationAcceptedEvent(
                    notification.getId(),
                    notification.getProject().getId(),
                    notification.getChannel(),
                    notification.getPriority(),
                    notification.getScheduledAt(),
                    notification.getCreatedAt()
                )))
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(Instant.now().plus(decision.delay()))
                .build());
            return;
        }

        notification.setStatus(NotificationStatus.FAILED);
        notificationRepository.save(notification);
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
            ? exception.getClass().getSimpleName()
            : message;
    }

    private String toJson(NotificationAcceptedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Unable to serialize notification retry event",
                exception
            );
        }
    }
}
