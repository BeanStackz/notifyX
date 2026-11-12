package com.beanstack.notifyx.project.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.project.entity.ProjectStatus;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String slug,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}