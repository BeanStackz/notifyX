package com.beanstack.notifyx.analytics.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(defaultValue = "30") int days
    ) {
        if (days != 7 && days != 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Analytics period must be 7 or 30 days");
        }
        return ResponseEntity.ok(ApiResponse.success(
            "Usage analytics retrieved successfully",
            analyticsService.getOrganizationAnalytics(organizationId, userDetails.getUsername(), days)
        ));
    }
}
