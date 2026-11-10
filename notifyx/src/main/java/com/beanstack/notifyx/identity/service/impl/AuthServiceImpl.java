package com.beanstack.notifyx.identity.service.impl;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.beanstack.notifyx.authorization.entity.Role;
import com.beanstack.notifyx.authorization.entity.RoleName;
import com.beanstack.notifyx.authorization.repository.RoleRepository;
import com.beanstack.notifyx.identity.dto.request.LoginRequest;
import com.beanstack.notifyx.identity.dto.request.RefreshTokenRequest;
import com.beanstack.notifyx.identity.dto.request.RegisterRequest;
import com.beanstack.notifyx.identity.dto.response.AuthResponse;
import com.beanstack.notifyx.identity.dto.response.UserResponse;
import com.beanstack.notifyx.identity.entity.RefreshToken;
import com.beanstack.notifyx.identity.entity.User;
import com.beanstack.notifyx.identity.mapper.UserMapper;
import com.beanstack.notifyx.identity.repository.RefreshTokenRepository;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.identity.security.JwtService;
import com.beanstack.notifyx.identity.service.AuthService;
import com.beanstack.notifyx.shared.exception.EmailAlreadyExistsException;
import com.beanstack.notifyx.shared.exception.InvalidTokenException;
import com.beanstack.notifyx.shared.exception.RefreshTokenReuseException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Value("${security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional 
    public AuthResponse login(LoginRequest request) {
        
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if(!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        return createAuthResponse(user, UUID.randomUUID());
    }

    @Override
    @Transactional 
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .ifPresent(token -> {
                if(!token.isRevoked() && token.getUsedAt() == null) {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                }
            }
        );
    }

    @Override
    @Transactional (dontRollbackOn = RefreshTokenReuseException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken currentToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token."));
        if(currentToken.isRevoked() || currentToken.getUsedAt() != null) {
            // Revoke all tokens for this user
            revokeTokenFamily(currentToken.getFamilyId());
            throw new RefreshTokenReuseException("Refresh token has been reused. All tokens for this user have been revoked.");
        }
        if(!currentToken.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired.");
        }

        User user = currentToken.getUser();
        if(!user.isEnabled()) {
            throw new InvalidTokenException("User account is disabled.");
        }

        String newRawToken = generateRefreshToken();
        String newTokenHash = hashToken(newRawToken);
        currentToken.setUsedAt(Instant.now());
        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByTokenHash(newTokenHash);

        RefreshToken replacement = RefreshToken.builder()
            .user(user)
            .tokenHash(newTokenHash)
            .familyId(currentToken.getFamilyId())
            .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
            .build();
        
        refreshTokenRepository.save(currentToken);
        refreshTokenRepository.save(replacement);

            String accessToken = jwtService.generateAccessToken(toUserDetails(user));

        return new AuthResponse(
            accessToken,
            newRawToken,
            "Bearer",
            jwtService.getAccessTokenExpirationSeconds(),
            userMapper.toUserResponse(user)
        );
    }

    @Override
    @Transactional 
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if(userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account with that email is already in use.");
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
            .orElseThrow(() -> new IllegalStateException("Default role not found."));
        
        User user = User.builder()
            .fullName(request.fullName().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .roles(Set.of(defaultRole))
            .enabled(true)
            .build();

        try{
            user = userRepository.save(user);
            return userMapper.toUserResponse(user);
        } catch(Exception e) {
            throw new EmailAlreadyExistsException("Failed to register user: " + e.getMessage());
        }
    }
    // Implementation of authentication service methods

    private AuthResponse createAuthResponse(User user, UUID familyId) {
        String accessToken = jwtService.generateAccessToken(toUserDetails(user));
        String refreshTokenRaw = generateRefreshToken();
        String refreshTokenHash = hashToken(refreshTokenRaw);

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash(refreshTokenHash)
            .familyId(familyId)
            .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
            .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken,
            refreshTokenRaw,
            "Bearer",
            jwtService.getAccessTokenExpirationSeconds(),
            userMapper.toUserResponse(user)
        );
    }

    private void revokeTokenFamily(UUID familyId) {
        
        var tokens = refreshTokenRepository.findAllByFamilyId(familyId);

        Instant now = Instant.now();
        tokens.forEach(token -> {
            if(!token.isRevoked() && token.getUsedAt() == null) {
                token.setRevokedAt(now);
            }
        });
        refreshTokenRepository.saveAll(tokens);
    }

    private org.springframework.security.core.userdetails.UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(user.getRoles().stream()
                .map(role -> role.getName().name())
                .toArray(String[]::new))
            .disabled(!user.isEnabled())
            .build();
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }


    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
    
}
