package com.beanstack.notifyx.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beanstack.notifyx.delivery.entity.ConsumerEvent;
import com.beanstack.notifyx.delivery.entity.ConsumerEventStatus;
import com.beanstack.notifyx.delivery.entity.DeliveryAttempt;
import com.beanstack.notifyx.delivery.entity.DeliveryStatus;
import com.beanstack.notifyx.delivery.repository.ConsumerEventRepository;
import com.beanstack.notifyx.delivery.repository.DeliveryAttemptRepository;
import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;
import com.beanstack.notifyx.notification.entity.NotificationStatus;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.outbox.entity.OutboxEvent;
import com.beanstack.notifyx.outbox.entity.OutboxStatus;
import com.beanstack.notifyx.outbox.repository.OutboxEventRepository;
import com.beanstack.notifyx.project.entity.Project;
import com.beanstack.notifyx.project.entity.ProjectStatus;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DeliveryRetryServiceTest {

    @Mock
    private DeliveryRetryPolicy retryPolicy;

    @Mock
    private DeliveryAttemptRepository deliveryAttemptRepository;

    @Mock
    private ConsumerEventRepository consumerEventRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private DeliveryRetryService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new DeliveryRetryService(
            retryPolicy,
            deliveryAttemptRepository,
            consumerEventRepository,
            notificationRepository,
            outboxEventRepository,
            objectMapper
        );
    }

    @Test
    void handleFailure_shouldRetryTransientFailureAndScheduleOutboxEvent() {
        UUID notificationId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        String eventId = "evt-123";
        RuntimeException exception = new RuntimeException("temporary outage");

        Notification notification = notification(notificationId);
        DeliveryAttempt attempt = deliveryAttempt(attemptId, notification, eventId, 2);
        ConsumerEvent consumerEvent = consumerEvent(notification, eventId);

        when(deliveryAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(consumerEventRepository.findByEventId(eventId)).thenReturn(Optional.of(consumerEvent));
        when(retryPolicy.evaluate(2, exception)).thenReturn(
            RetryDecision.retryAfter(Duration.ofMinutes(5))
        );

        service.handleFailure(attemptId, exception);

        assertEquals(DeliveryStatus.FAILED, attempt.getStatus());
        assertEquals("temporary outage", attempt.getErrorMessage());
        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        assertEquals(ConsumerEventStatus.FAILED, consumerEvent.getStatus());
        assertEquals("temporary outage", consumerEvent.getLastError());

        verify(outboxEventRepository).save(argThat(event ->
            event.getAggregateId().equals(notificationId) &&
            "NOTIFICATION_ACCEPTED".equals(event.getEventType()) &&
            event.getStatus() == OutboxStatus.PENDING &&
            event.getNextAttemptAt() != null &&
            event.getPayload() != null &&
            event.getPayload().contains("\"notificationId\"")
        ));
    }

    @Test
    void handleFailure_shouldFailNotificationWhenRetryIsNotAllowed() {
        UUID notificationId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        String eventId = "evt-456";
        RuntimeException exception = new RuntimeException("permanent error");

        Notification notification = notification(notificationId);
        DeliveryAttempt attempt = deliveryAttempt(attemptId, notification, eventId, 1);
        ConsumerEvent consumerEvent = consumerEvent(notification, eventId);

        when(deliveryAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(consumerEventRepository.findByEventId(eventId)).thenReturn(Optional.of(consumerEvent));
        when(retryPolicy.evaluate(1, exception)).thenReturn(RetryDecision.permanentFailure());

        service.handleFailure(attemptId, exception);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(ConsumerEventStatus.FAILED, consumerEvent.getStatus());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void handleFailure_shouldHandleExhaustedRetriesAsTerminalFailure() {
        UUID notificationId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        String eventId = "evt-789";
        RuntimeException exception = new RuntimeException("still broken");

        Notification notification = notification(notificationId);
        DeliveryAttempt attempt = deliveryAttempt(attemptId, notification, eventId, 5);
        ConsumerEvent consumerEvent = consumerEvent(notification, eventId);

        when(deliveryAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(consumerEventRepository.findByEventId(eventId)).thenReturn(Optional.of(consumerEvent));
        when(retryPolicy.evaluate(5, exception)).thenReturn(RetryDecision.attemptsExhausted());

        service.handleFailure(attemptId, exception);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(ConsumerEventStatus.FAILED, consumerEvent.getStatus());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    private Notification notification(UUID notificationId) {
        Organization organization = Organization.builder()
            .id(UUID.randomUUID())
            .name("Acme Org")
            .slug("acme-org")
            .enabled(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Project project = Project.builder()
            .id(UUID.randomUUID())
            .organization(organization)
            .name("Test Project")
            .slug("test-project")
            .description("project")
            .status(ProjectStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        return Notification.builder()
            .id(notificationId)
            .project(project)
            .idempotencyKey("key-1")
            .requestHash("hash")
            .channel(NotificationChannel.EMAIL)
            .recipient("alice@example.com")
            .subject("Welcome")
            .content("Hello world")
            .metadata(Map.of("emailTemplate", Map.of("id", "minimal")))
            .status(NotificationStatus.ACCEPTED)
            .priority(NotificationPriority.NORMAL)
            .scheduledAt(Instant.now().plusSeconds(120))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private DeliveryAttempt deliveryAttempt(
        UUID attemptId,
        Notification notification,
        String eventId,
        int attemptNumber
    ) {
        DeliveryAttempt attempt = DeliveryAttempt.builder()
            .id(attemptId)
            .notification(notification)
            .eventId(eventId)
            .attemptNumber(attemptNumber)
            .status(DeliveryStatus.STARTED)
            .startedAt(Instant.now())
            .build();
        return attempt;
    }

    private ConsumerEvent consumerEvent(Notification notification, String eventId) {
        return ConsumerEvent.builder()
            .id(UUID.randomUUID())
            .eventId(eventId)
            .notification(notification)
            .status(ConsumerEventStatus.PROCESSING)
            .claimedAt(Instant.now())
            .build();
    }
}
