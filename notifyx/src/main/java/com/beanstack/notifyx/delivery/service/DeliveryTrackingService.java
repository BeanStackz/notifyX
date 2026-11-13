package com.beanstack.notifyx.delivery.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.delivery.dto.DeliveryTrackingResponse;
import com.beanstack.notifyx.delivery.repository.*;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
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
        var notification = notificationRepository.findById(notificationId)
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
}
