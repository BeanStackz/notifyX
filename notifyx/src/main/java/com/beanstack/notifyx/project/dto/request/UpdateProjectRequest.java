package com.beanstack.notifyx.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Replacement values for an existing project.")
public record UpdateProjectRequest(
    
    @Schema(description = "Updated human-readable project name.", example = "Transactional email")
    @NotBlank(message = "Project name is required")
        @Size(max = 150)
        String name,

        @Schema(description = "Updated project slug, unique within its organization.", example = "transactional-email")
        @NotBlank(message = "Project slug is required")
        @Size(max = 100)
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*$",
                message = "Invalid project slug"
        )
        String slug,

        @Schema(description = "Updated optional project purpose or notes. Maximum 500 characters.", example = "Production email notifications")
        @Size(max = 500)
        String description
) {
    
}
