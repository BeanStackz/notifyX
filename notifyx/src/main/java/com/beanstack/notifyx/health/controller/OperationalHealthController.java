package com.beanstack.notifyx.health.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.health.dto.OperationalHealthResponse;
import com.beanstack.notifyx.health.service.OperationalHealthService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Operational health", description = "Backend dependency status and queue backlog indicators.")
@SecurityRequirement(name = "bearerAuth")
public class OperationalHealthController {

    private final OperationalHealthService healthService;

    @GetMapping("/operations")
    @Operation(summary = "Get operational health", description = "Reports RabbitMQ and Brevo status, pending outbox backlog, and dead-letter queue count.")
    public ResponseEntity<ApiResponse<OperationalHealthResponse>> operations() {
        return ResponseEntity.ok(ApiResponse.success(
            "Operational health retrieved successfully",
            healthService.getHealth()
        ));
    }
}
