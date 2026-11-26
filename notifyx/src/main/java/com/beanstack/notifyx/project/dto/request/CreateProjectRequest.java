package com.beanstack.notifyx.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details required to create an organization-scoped project.")
public record CreateProjectRequest(

    @Schema(description = "Human-readable project name.", example = "Transactional email")
    @NotBlank (message = "Project name is required")
    @Size (max = 150, message = "Project name must not exceed 150 characters")
    String name,

    @Schema(description = "Lowercase project identifier, unique within its organization.", example = "transactional-email")
    @NotBlank (message = "Project slug is required")
    @Size (max = 100, message = "Project slug must not exceed 100 characters")
    @Pattern (
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "Project slug must be lowercase and can only contain letters, numbers, and hyphens"
    )
    String slug,

    @Schema(description = "Optional project purpose or notes. Maximum 500 characters.", example = "Production email notifications")
    @Size (max = 500, message = "Project description must not exceed 500 characters")
    String description

) {
    
}
