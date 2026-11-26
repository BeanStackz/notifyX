package com.beanstack.notifyx.apikey.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.apikey.security.ApiKeyPrincipal;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/ingest")
@Tag(name = "Notification ingestion", description = "Project API-key authenticated endpoints for notification clients.")
@SecurityRequirement(name = "apiKeyAuth")
public class ApiKeyTestController {

    @GetMapping("/whoami")
    @Operation(summary = "Check API key", description = "Returns the project identity associated with the supplied API key.")
    public ResponseEntity<ApiResponse<ApiKeyPrincipal>> whoAmI(
        @AuthenticationPrincipal ApiKeyPrincipal principal
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "API key authenticated successfully",
                principal
            )
        );
    }
}