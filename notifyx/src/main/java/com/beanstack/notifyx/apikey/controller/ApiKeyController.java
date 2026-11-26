package com.beanstack.notifyx.apikey.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.apikey.dto.request.CreateApiKeyRequest;
import com.beanstack.notifyx.apikey.dto.response.ApiKeyCreatedResponse;
import com.beanstack.notifyx.apikey.dto.response.ApiKeyResponse;
import com.beanstack.notifyx.apikey.service.ApiKeyService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/projects/{projectId}/api-keys")
@RequiredArgsConstructor
@Tag(name = "API keys", description = "Issue and manage project-scoped notification ingestion credentials.")
@SecurityRequirement(name = "bearerAuth")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Create API key", description = "Creates a project API key. The secret is returned only in this response; store it securely.")
    public ResponseEntity<ApiResponse<ApiKeyCreatedResponse>> createApiKey(
        @PathVariable UUID organizationId,
        @PathVariable UUID projectId,
        @Valid @RequestBody CreateApiKeyRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(
            organizationId,
            projectId,
            userDetails.getUsername(),
            request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("API key created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List API keys", description = "Lists API key metadata for a project. Secret key values are never returned.")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getApiKeys(
        @PathVariable UUID organizationId,
        @PathVariable UUID projectId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<ApiKeyResponse> response = apiKeyService.getProjectApiKeys(
            organizationId,
            projectId,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(
            ApiResponse.success("API keys retrieved successfully", response)
        );
    }

    @DeleteMapping("/{apiKeyId}")
    @Operation(summary = "Revoke API key", description = "Revokes an API key belonging to the specified project.")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> revokeApiKey(
        @PathVariable UUID organizationId,
        @PathVariable UUID projectId,
        @PathVariable UUID apiKeyId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        ApiKeyResponse response = apiKeyService.revokeApiKey(
            organizationId,
            projectId,
            apiKeyId,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(
            ApiResponse.success("API key revoked successfully", response)
        );
    }
}
