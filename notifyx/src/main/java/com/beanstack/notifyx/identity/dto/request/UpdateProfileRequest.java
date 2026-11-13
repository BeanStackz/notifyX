package com.beanstack.notifyx.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String fullName
) {}
