package com.beanstack.notifyx.project.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.project.entity.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project identity, organization, descriptive fields, lifecycle status, and timestamps.")
public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String slug,
        @Schema(description = "Optional project purpose or notes.")
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}