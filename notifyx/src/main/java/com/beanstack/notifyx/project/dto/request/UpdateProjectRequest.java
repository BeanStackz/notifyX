package com.beanstack.notifyx.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    
    @NotBlank(message = "Project name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Project slug is required")
        @Size(max = 100)
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*$",
                message = "Invalid project slug"
        )
        String slug,

        @Size(max = 500)
        String description
) {
    
}
