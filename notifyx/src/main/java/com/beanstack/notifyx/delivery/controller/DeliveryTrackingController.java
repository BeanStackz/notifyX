package com.beanstack.notifyx.delivery.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.beanstack.notifyx.delivery.dto.DeliveryTrackingResponse;
import com.beanstack.notifyx.notification.dto.response.NotificationResponse;
import com.beanstack.notifyx.delivery.service.DeliveryTrackingService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/notifications")
@RequiredArgsConstructor
@Tag(name = "Delivery tracking", description = "Review notification state and delivery attempts, and request retries.")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryTrackingController {
    private final DeliveryTrackingService trackingService;

    @GetMapping
    @Operation(summary = "List project notifications", description = "Lists notification history for a project accessible to the authenticated user.")
    public ApiResponse<List<NotificationResponse>> listNotifications(
        @PathVariable UUID projectId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.success(
            "Notification history retrieved successfully",
            trackingService.list(projectId, userDetails.getUsername())
        );
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get delivery tracking", description = "Returns a notification's state, provider attempts, and delivery lifecycle events.")
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

    @PostMapping("/{notificationId}/retry")
    @Operation(summary = "Retry notification", description = "Queues another delivery attempt for a notification in the specified project.")
    public ApiResponse<Void> retry(
        @PathVariable UUID projectId,
        @PathVariable UUID notificationId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        trackingService.retry(
            projectId,
            notificationId,
            userDetails.getUsername()
        );
        return ApiResponse.success("Notification retry queued successfully", null);
    }
}
