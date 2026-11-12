package com.beanstack.notifyx.apikey.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.apikey.entity.ApiKeyStatus;

public record ApiKeyCreatedResponse(
    UUID id,
    UUID projectId,
    String name,
    String keyPrefix,
    String apiKey,
    ApiKeyStatus status,
    Instant expiresAt,
    Instant createdAt
) {
    
}
