package com.beanstack.notifyx.apikey.security;

import java.util.UUID;

public record ApiKeyPrincipal(
    UUID apiKeyId,
    UUID projectId,
    UUID organizationId
) {
    
}
