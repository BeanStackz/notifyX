package com.beanstack.notifyx.apikey.service;

import java.util.List;
import java.util.UUID;

import com.beanstack.notifyx.apikey.dto.request.CreateApiKeyRequest;
import com.beanstack.notifyx.apikey.dto.response.ApiKeyCreatedResponse;
import com.beanstack.notifyx.apikey.dto.response.ApiKeyResponse;

public interface ApiKeyService {
    
    ApiKeyCreatedResponse createApiKey(
        UUID organizationId,
        UUID projectId,
        String userEmail,
        CreateApiKeyRequest request
    );

    List<ApiKeyResponse> getProjectApiKeys(
        UUID organizationId,
        UUID projectId,
        String userEmail
    );

    ApiKeyResponse revokeApiKey(
        UUID organizationId,
        UUID projectId,
        UUID apiKeyId,
        String userEmail
    );
}
