package com.beanstack.notifyx.analytics.service;

import java.util.LinkedHashMap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.beanstack.notifyx.analytics.dto.UsageAnalyticsResponse;
import com.beanstack.notifyx.analytics.dto.DailyNotificationPoint;
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

    public UsageAnalyticsResponse getOrganizationAnalytics(UUID organizationId, String email, int days) {
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
        long deliveredNotifications = statuses.getOrDefault("SENT", 0L);
        long failedNotifications = statuses.getOrDefault("FAILED", 0L);
        long terminalNotifications = deliveredNotifications + failedNotifications;
        double deliveryRate = terminalNotifications == 0
            ? 0
            : Math.round((deliveredNotifications * 10000.0) / terminalNotifications) / 100.0;
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate startDate = endDate.minusDays(days);
        Instant from = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        var counts = notificationRepository.countDailyForOrganization(organizationId, from, to)
            .stream()
            .collect(Collectors.toMap(row -> row[0].toString(), row -> ((Number) row[1]).longValue()));
        List<DailyNotificationPoint> dailySeries = new ArrayList<>();
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            String key = date.toString();
            dailySeries.add(new DailyNotificationPoint(key, counts.getOrDefault(key, 0L)));
        }
        return new UsageAnalyticsResponse(
            notificationRepository.countByProject_Organization_Id(organizationId),
            projectRepository.findAllByOrganizationId(organizationId).size(),
            statuses,
            channels,
            dailySeries,
            deliveredNotifications,
            failedNotifications,
            deliveryRate
        );
    }
}
