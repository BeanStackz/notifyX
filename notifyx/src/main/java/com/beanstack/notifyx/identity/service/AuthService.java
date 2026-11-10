package com.beanstack.notifyx.identity.service;

import com.beanstack.notifyx.identity.dto.request.LoginRequest;
import com.beanstack.notifyx.identity.dto.request.RefreshTokenRequest;
import com.beanstack.notifyx.identity.dto.request.RegisterRequest;
import com.beanstack.notifyx.identity.dto.response.AuthResponse;
import com.beanstack.notifyx.identity.dto.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
