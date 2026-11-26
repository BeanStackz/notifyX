package com.beanstack.notifyx.identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.identity.dto.request.LoginRequest;
import com.beanstack.notifyx.identity.dto.request.RefreshTokenRequest;
import com.beanstack.notifyx.identity.dto.request.RegisterRequest;
import com.beanstack.notifyx.identity.dto.response.AuthResponse;
import com.beanstack.notifyx.identity.dto.response.UserResponse;
import com.beanstack.notifyx.identity.service.AuthService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/auth")
@RequiredArgsConstructor 
@Tag(name = "Authentication", description = "Register users and obtain, refresh, or revoke JWT sessions.")
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a user", description = "Creates a user account. This endpoint is public.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User Registered Successfully!", userResponse));
    }

    @PostMapping ("/login")
    @Operation(summary = "Log in", description = "Validates credentials and returns access and refresh tokens. This endpoint is public.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping ("/refresh")
    @Operation(summary = "Refresh tokens", description = "Exchanges a valid refresh token for a new token pair. This endpoint is public.")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    @PostMapping ("/logout")
    @Operation(
        summary = "Log out",
        description = "Revokes the supplied refresh token for the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
