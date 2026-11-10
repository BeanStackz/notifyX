package com.beanstack.notifyx.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
    
}
