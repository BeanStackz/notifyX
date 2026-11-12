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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ingest/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
        @AuthenticationPrincipal ApiKeyPrincipal principal,
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