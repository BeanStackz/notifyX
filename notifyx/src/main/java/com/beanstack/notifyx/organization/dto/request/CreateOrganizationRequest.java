package com.beanstack.notifyx.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest (
    @NotBlank (message = "Organization name is required")
    @Size (max = 100, message = "Organization name must be less than 100 characters")
    String name,

    @NotBlank (message = "Organization slug is required")
    @Size (max = 100, message = "Organization slug must be less than 100 characters")
    @Pattern (
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "Organization slug must be lowercase alphanumeric and can contain hyphens"
    )
    String slug
) {
    
}
