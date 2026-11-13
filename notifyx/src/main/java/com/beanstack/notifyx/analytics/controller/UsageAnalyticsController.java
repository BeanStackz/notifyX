package com.beanstack.notifyx.analytics.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.analytics.dto.UsageAnalyticsResponse;
import com.beanstack.notifyx.analytics.service.UsageAnalyticsService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/analytics")
@RequiredArgsConstructor
public class UsageAnalyticsController {

    private final UsageAnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<UsageAnalyticsResponse>> getAnalytics(
        @PathVariable UUID organizationId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Usage analytics retrieved successfully",
            analyticsService.getOrganizationAnalytics(organizationId, userDetails.getUsername())
        ));
    }
}
