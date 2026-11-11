package com.beanstack.notifyx.organization.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.organization.entity.OrganizationRole;

public record OrganizationResponse (
    UUID id,
    String name,
    String slug,
    OrganizationRole role,
    Instant createdAt
) {
    
}
