package com.beanstack.notifyx.analytics.service;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.beanstack.notifyx.analytics.dto.UsageAnalyticsResponse;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
import com.beanstack.notifyx.organization.repository.OrganizationMemberRepository;
import com.beanstack.notifyx.project.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsageAnalyticsService {

    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectRepository projectRepository;

    public UsageAnalyticsResponse getOrganizationAnalytics(UUID organizationId, String email) {
        UUID userId = userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("User not found"))
            .getId();
        if (!memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new AccessDeniedException("You do not belong to this organization");
        }

        var statuses = notificationRepository.countByStatusForOrganization(organizationId)
            .stream().collect(Collectors.toMap(row -> row[0].toString(), row -> ((Number) row[1]).longValue(), (a, b) -> b, LinkedHashMap::new));
        var channels = notificationRepository.countByChannelForOrganization(organizationId)
            .stream().collect(Collectors.toMap(row -> row[0].toString(), row -> ((Number) row[1]).longValue(), (a, b) -> b, LinkedHashMap::new));
        return new UsageAnalyticsResponse(
            notificationRepository.countByProject_Organization_Id(organizationId),
            projectRepository.findAllByOrganizationId(organizationId).size(),
            statuses,
            channels
        );
    }
}
