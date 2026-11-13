package com.beanstack.notifyx.delivery.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.beanstack.notifyx.delivery.dto.DeliveryTrackingResponse;
import com.beanstack.notifyx.delivery.service.DeliveryTrackingService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/notifications")
@RequiredArgsConstructor
public class DeliveryTrackingController {
    private final DeliveryTrackingService trackingService;

    @GetMapping("/{notificationId}")
    public ApiResponse<DeliveryTrackingResponse> getTracking(
        @PathVariable UUID projectId,
        @PathVariable UUID notificationId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.success(
            "Notification delivery tracking retrieved successfully",
            trackingService.get(projectId, notificationId, userDetails.getUsername())
        );
    }
}
