package com.beanstack.notifyx.apikey.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.apikey.entity.ApiKeyStatus;

public record ApiKeyResponse(
    UUID id,
    UUID projectId,
    String name,
    String keyPrefix,
    ApiKeyStatus status,
    Instant expiresAt,
    Instant lastUsedAt,
    Instant createdAt,
    Instant revokedAt
) {
    
}
