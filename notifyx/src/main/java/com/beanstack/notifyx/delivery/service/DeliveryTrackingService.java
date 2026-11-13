package com.beanstack.notifyx.delivery.service;

import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.delivery.dto.DeliveryTrackingResponse;
import com.beanstack.notifyx.delivery.repository.*;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
import com.beanstack.notifyx.notification.dto.response.NotificationResponse;
import com.beanstack.notifyx.notification.entity.NotificationStatus;
import com.beanstack.notifyx.outbox.dto.NotificationAcceptedEvent;
import com.beanstack.notifyx.outbox.entity.OutboxEvent;
import com.beanstack.notifyx.outbox.entity.OutboxStatus;
import com.beanstack.notifyx.outbox.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import com.beanstack.notifyx.organization.repository.OrganizationMemberRepository;
import com.beanstack.notifyx.project.repository.ProjectRepository;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryTrackingService {
    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DeliveryEventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void retry(
        UUID projectId,
        UUID notificationId,
        String email
    ) {
        var project = verifyProjectAccess(projectId, email);
        var notification = notificationRepository.findById(notificationId)
            .filter(value -> value.getProject().getId().equals(project.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException(
                "Only failed notifications can be retried"
            );
        }

        var event = new NotificationAcceptedEvent(
            notification.getId(),
            project.getId(),
            notification.getChannel(),
            notification.getPriority(),
            notification.getScheduledAt(),
            notification.getCreatedAt()
        );

        try {
            notification.setStatus(NotificationStatus.RETRYING);
            notificationRepository.save(notification);
            outboxEventRepository.save(
                OutboxEvent.builder()
                    .aggregateId(notification.getId())
                    .eventType("NOTIFICATION_ACCEPTED")
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .attempts(0)
                    .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Unable to queue notification retry",
                exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(
        UUID projectId,
        String email
    ) {
        verifyProjectAccess(projectId, email);
        return notificationRepository
            .findAllWithProjectByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .map(value -> new NotificationResponse(
                value.getId(),
                value.getProject().getId(),
                value.getChannel(),
                value.getRecipient(),
                value.getStatus(),
                value.getPriority(),
                value.getScheduledAt(),
                value.getCreatedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryTrackingResponse get(
        UUID projectId,
        UUID notificationId,
        String email
    ) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        var project = projectRepository.findById(projectId)
            .filter(value -> memberRepository.existsByOrganizationIdAndUserId(
                value.getOrganization().getId(), user.getId()
            ))
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        var notification = notificationRepository
            .findWithProjectAndOrganizationById(notificationId)
            .filter(value -> value.getProject().getId().equals(project.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        var attempts = attemptRepository
            .findAllByNotification_IdOrderByAttemptNumberAsc(notificationId)
            .stream()
            .map(value -> new DeliveryTrackingResponse.Attempt(
                value.getId(),
                value.getAttemptNumber(),
                value.getStatus(),
                value.getProviderMessageId(),
                value.getProviderResponse(),
                value.getErrorMessage(),
                value.getStartedAt(),
                value.getCompletedAt()
            ))
            .toList();
        var events = eventRepository
            .findAllByNotification_IdOrderByOccurredAtAsc(notificationId)
            .stream()
            .map(value -> new DeliveryTrackingResponse.Event(
                value.getEventType(),
                value.getProviderMessageId(),
                value.getRecipient(),
                value.getOccurredAt(),
                value.getReceivedAt()
            ))
            .toList();

        return new DeliveryTrackingResponse(
            notification.getId(),
            project.getId(),
            notification.getChannel(),
            notification.getRecipient(),
            notification.getStatus(),
            notification.getPriority(),
            notification.getScheduledAt(),
            notification.getCreatedAt(),
            attempts,
            events
        );
    }

    private com.beanstack.notifyx.project.entity.Project verifyProjectAccess(UUID projectId, String email) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return projectRepository.findById(projectId)
            .filter(value -> memberRepository.existsByOrganizationIdAndUserId(
                value.getOrganization().getId(), user.getId()
            ))
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
}
