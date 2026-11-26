package com.beanstack.notifyx.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.apikey.security.ApiKeyPrincipal;
import com.beanstack.notifyx.notification.dto.request.CreateNotificationRequest;
import com.beanstack.notifyx.notification.dto.response.NotificationResponse;
import com.beanstack.notifyx.notification.service.NotificationService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ingest/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification ingestion", description = "Project API-key authenticated endpoints for notification clients.")
@SecurityRequirement(name = "apiKeyAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(
        summary = "Submit notification",
        description = "Accepts a notification for asynchronous delivery. The project is derived from the API key. The Idempotency-Key prevents duplicate creation within that project. scheduledAt is stored but does not currently defer publication."
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
        @AuthenticationPrincipal ApiKeyPrincipal principal,
        @Parameter(description = "Client-generated idempotency key unique within the project.", required = true)
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateNotificationRequest request
    ) {
        NotificationResponse response =
            notificationService.createNotification(
                principal.projectId(),
                idempotencyKey,
                request
            );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(
                ApiResponse.success(
                    "Notification accepted successfully",
                    response
                )
            );
    }
}