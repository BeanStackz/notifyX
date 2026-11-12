package com.beanstack.notifyx.notification.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.beanstack.notifyx.notification.dto.request.CreateNotificationRequest;
import com.beanstack.notifyx.notification.dto.response.NotificationResponse;
import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.entity.NotificationPriority;
import com.beanstack.notifyx.notification.entity.NotificationStatus;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
import com.beanstack.notifyx.notification.service.NotificationService;
import com.beanstack.notifyx.outbox.dto.NotificationAcceptedEvent;
import com.beanstack.notifyx.outbox.entity.OutboxEvent;
import com.beanstack.notifyx.outbox.entity.OutboxStatus;
import com.beanstack.notifyx.outbox.repository.OutboxEventRepository;
import com.beanstack.notifyx.project.entity.Project;
import com.beanstack.notifyx.project.entity.ProjectStatus;
import com.beanstack.notifyx.project.repository.ProjectRepository;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;
import com.beanstack.notifyx.subscription.service.SubscriptionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String EVENT_TYPE = "NOTIFICATION_ACCEPTED";

    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;
    private final SubscriptionService subscriptionService;
    

    @Override
    @Transactional
    public NotificationResponse createNotification(
        UUID projectId,
        String idempotencyKey,
        CreateNotificationRequest request
    ) {
        if (idempotencyKey == null ||
            idempotencyKey.isBlank() ||
            idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                "A valid Idempotency-Key header is required"
            );
        }

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Project not found")
            );

        if (project.getStatus() != ProjectStatus.ACTIVE ||
            !project.getOrganization().isEnabled()) {
            throw new ResourceNotFoundException("Project not found");
        }

        String requestHash = calculateRequestHash(request);
        validateSchedule(request);

        var existing = notificationRepository
            .findByProject_IdAndIdempotencyKey(projectId, idempotencyKey);

        

        NotificationPriority priority =
            subscriptionService.reserveNotificationQuota(
                project.getOrganization().getId(),
                request.channel()
        );

        if (existing.isPresent()) {
            Notification notification = existing.get();

            if (!notification.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException(
                    "Idempotency key was already used with a different request"
                );
            }

            return toResponse(notification);
        }


        Instant now = Instant.now();

        Notification notification = Notification.builder()
            .project(project)
            .idempotencyKey(idempotencyKey)
            .requestHash(requestHash)
            .channel(request.channel())
            .recipient(request.recipient().trim())
            .subject(request.subject())
            .content(request.content())
            .metadata(request.metadata())
            .status(NotificationStatus.ACCEPTED)
            .priority(priority)
            .scheduledAt(request.scheduledAt())
            .build();

        Notification savedNotification =
            notificationRepository.saveAndFlush(notification);

        NotificationAcceptedEvent event = new NotificationAcceptedEvent(
            savedNotification.getId(),
            project.getId(),
            savedNotification.getChannel(),
            savedNotification.getPriority(),
            savedNotification.getScheduledAt(),
            now
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
            .aggregateId(savedNotification.getId())
            .eventType(EVENT_TYPE)
            .payload(toJson(event))
            .status(OutboxStatus.PENDING)
            .attempts(0)
            .build();

        outboxEventRepository.save(outboxEvent);

        return toResponse(savedNotification);
    }

    private void validateSchedule(CreateNotificationRequest request) {
        if (request.scheduledAt() != null &&
            !request.scheduledAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                "scheduledAt must be a future timestamp"
            );
        }
    }

    private String calculateRequestHash(
        CreateNotificationRequest request
    ) {
        try {
            var canonicalRequest = new TreeMap<String, Object>();

            canonicalRequest.put("channel", request.channel());
            canonicalRequest.put("recipient", request.recipient().trim());
            canonicalRequest.put("subject", request.subject());
            canonicalRequest.put("content", request.content());
            canonicalRequest.put(
                "metadata",
                request.metadata() == null
                    ? null
                    : new TreeMap<>(request.metadata())
            );
            canonicalRequest.put("scheduledAt", request.scheduledAt());

            String canonicalJson =
                objectMapper.writeValueAsString(canonicalRequest);

            return sha256(canonicalJson);

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Unable to fingerprint notification request",
                exception
            );
        }
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 algorithm is unavailable",
                exception
            );
        }
    }

    private String toJson(NotificationAcceptedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Unable to serialize notification event",
                exception
            );
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getProject().getId(),
            notification.getChannel(),
            notification.getRecipient(),
            notification.getStatus(),
            notification.getPriority(),
            notification.getScheduledAt(),
            notification.getCreatedAt()
        );
    }
}