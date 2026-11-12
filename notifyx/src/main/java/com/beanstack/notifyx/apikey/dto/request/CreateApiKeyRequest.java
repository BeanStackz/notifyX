package com.beanstack.notifyx.apikey.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
    @NotBlank (message = "Name is required")
    @Size (max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Min (value = 1, message = "Expiration days must be at least 1 day")
    @Max (value = 3650, message = "Expiration days must not exceed 3650 days")
    Integer expiresInDays
) {
    
}
