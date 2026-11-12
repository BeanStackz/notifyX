package com.beanstack.notifyx.apikey.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.apikey.entity.ApiKey;
import com.beanstack.notifyx.apikey.entity.ApiKeyStatus;
import com.beanstack.notifyx.apikey.repository.ApiKeyRepository;
import com.beanstack.notifyx.project.entity.ProjectStatus;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ApiKeyAuthenticationService {
    private static final String KEY_PREFIX = "ntx_live_";

    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public Optional<ApiKeyPrincipal> authenticate(String rawApiKey) {

        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }

        String[] parts = rawApiKey.split("_", 4);

        if (parts.length != 4 ||
            !parts[0].equals("ntx") ||
            !parts[1].equals("live") ||
            !parts[2].matches("[a-f0-9]{16}") ||
            parts[3].isBlank()) {

            return Optional.empty();
        }

        String keyId = parts[2];

        Optional<ApiKey> optionalApiKey =
            apiKeyRepository.findByKeyId(keyId);

        if (optionalApiKey.isEmpty()) {
            return Optional.empty();
        }

        ApiKey apiKey = optionalApiKey.get();

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            return Optional.empty();
        }

        Instant now = Instant.now();

        if (apiKey.getExpiresAt() != null &&
            !apiKey.getExpiresAt().isAfter(now)) {

            return Optional.empty();
        }

        if (apiKey.getProject().getStatus() != ProjectStatus.ACTIVE) {
            return Optional.empty();
        }

        if (!apiKey.getProject().getOrganization().isEnabled()) {
            return Optional.empty();
        }

        String suppliedHash = hashApiKey(rawApiKey);

        byte[] storedHashBytes = HexFormat.of().parseHex(
            apiKey.getKeyHash()
        );

        byte[] suppliedHashBytes = HexFormat.of().parseHex(
            suppliedHash
        );

        if (!MessageDigest.isEqual(
            storedHashBytes,
            suppliedHashBytes
        )) {
            return Optional.empty();
        }

        apiKeyRepository.updateLastUsedAtIfOlder(
            apiKey.getId(),
            now,
            now.minus(5, ChronoUnit.MINUTES)
        );

        return Optional.of(
            new ApiKeyPrincipal(
                apiKey.getId(),
                apiKey.getProject().getId(),
                apiKey.getProject().getOrganization().getId()
            )
        );
    }

    private String hashApiKey(String rawApiKey) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                rawApiKey.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 algorithm is unavailable",
                exception
            );
        }
    }
}
