package com.beanstack.notifyx.apikey.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.apikey.dto.request.CreateApiKeyRequest;
import com.beanstack.notifyx.apikey.dto.response.ApiKeyCreatedResponse;
import com.beanstack.notifyx.apikey.dto.response.ApiKeyResponse;
import com.beanstack.notifyx.apikey.entity.ApiKey;
import com.beanstack.notifyx.apikey.entity.ApiKeyStatus;
import com.beanstack.notifyx.apikey.repository.ApiKeyRepository;
import com.beanstack.notifyx.apikey.service.ApiKeyService;
import com.beanstack.notifyx.identity.entity.User;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.organization.entity.OrganizationMember;
import com.beanstack.notifyx.organization.entity.OrganizationRole;
import com.beanstack.notifyx.organization.repository.OrganizationMemberRepository;
import com.beanstack.notifyx.organization.repository.OrganizationRepository;
import com.beanstack.notifyx.project.entity.Project;
import com.beanstack.notifyx.project.entity.ProjectStatus;
import com.beanstack.notifyx.project.repository.ProjectRepository;
import com.beanstack.notifyx.shared.exception.ApiKeyAlreadyRevokedException;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final String KEY_PREFIX = "ntx_live_";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional 
    public ApiKeyCreatedResponse createApiKey(
            UUID organizationId, 
            UUID projectId, 
            String userEmail,
            CreateApiKeyRequest request) {

            User user = findUser(userEmail);
            Organization organization = findOrganization(organizationId);

        if (!organization.isEnabled()) {
            throw new AccessDeniedException("Organization is disabled");
        }

        OrganizationMember membership =
            findMembership(organizationId, user.getId());

        requireManagementPermission(membership);

        Project project = findProject(organizationId, projectId);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Cannot create API keys for an archived project"
            );
        }

        String keyId = generateKeyId();
        String rawApiKey = generateRawApiKey(keyId);

        String keyHash = hashApiKey(rawApiKey);

        Instant expiresAt = request.expiresInDays() == null
            ? null
            : Instant.now().plus(request.expiresInDays(), ChronoUnit.DAYS);

        ApiKey apiKey = ApiKey.builder()
            .project(project)
            .keyId(keyId)
            .keyHash(keyHash)
            .name(request.name().trim())
            .status(ApiKeyStatus.ACTIVE)
            .createdBy(user)
            .expiresAt(expiresAt)
            .build();

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        return new ApiKeyCreatedResponse(
            savedApiKey.getId(),
            project.getId(),
            savedApiKey.getName(),
            KEY_PREFIX + keyId,
            rawApiKey,
            savedApiKey.getStatus(),
            savedApiKey.getExpiresAt(),
            savedApiKey.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getProjectApiKeys(UUID organizationId, UUID projectId, String userEmail) {
        User user = findUser(userEmail);

        Organization organization = findOrganization(organizationId);

        if (!organization.isEnabled()) {
            throw new AccessDeniedException("Organization is disabled");
        }

        OrganizationMember membership =
            findMembership(organizationId, user.getId());

        requireManagementPermission(membership);

        Project project = findProject(organizationId, projectId);

        return apiKeyRepository
            .findAllByProjectIdOrderByCreatedAtDesc(project.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public ApiKeyResponse revokeApiKey(UUID organizationId, UUID projectId, UUID apiKeyId, String userEmail) {
        User user = findUser(userEmail);

        Organization organization = findOrganization(organizationId);

        if (!organization.isEnabled()) {
            throw new AccessDeniedException("Organization is disabled");
        }

        OrganizationMember membership =
            findMembership(organizationId, user.getId());

        requireManagementPermission(membership);

        Project project = findProject(organizationId, projectId);

        ApiKey apiKey = apiKeyRepository
            .findByIdAndProjectId(apiKeyId, project.getId())
            .orElseThrow(() ->
                new ResourceNotFoundException("API key not found")
            );

        if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
            throw new ApiKeyAlreadyRevokedException("API key is already revoked");
        }

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(Instant.now());

        return toResponse(apiKey);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found")
            );
    }

    private Organization findOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Organization not found")
            );
    }

    private OrganizationMember findMembership(
        UUID organizationId,
        UUID userId
    ) {
        return organizationMemberRepository
            .findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Organization not found")
            );
    }

    private Project findProject(UUID organizationId, UUID projectId) {
        return projectRepository
            .findByIdAndOrganizationId(projectId, organizationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Project not found")
            );
    }

    private void requireManagementPermission(
        OrganizationMember membership
    ) {
        OrganizationRole role = membership.getRole();

        if (role != OrganizationRole.OWNER &&
            role != OrganizationRole.ADMIN) {
            throw new AccessDeniedException(
                "You do not have permission to manage API keys"
            );
        }
    }

    private String generateKeyId() {
        byte[] bytes = new byte[8];
        SECURE_RANDOM.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    private String generateRawApiKey(String keyId) {
        byte[] secretBytes = new byte[32];
        SECURE_RANDOM.nextBytes(secretBytes);

        String secret = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(secretBytes);

        return KEY_PREFIX + keyId + "_" + secret;
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

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            apiKey.getProject().getId(),
            apiKey.getName(),
            KEY_PREFIX + apiKey.getKeyId(),
            apiKey.getStatus(),
            apiKey.getExpiresAt(),
            apiKey.getLastUsedAt(),
            apiKey.getCreatedAt(),
            apiKey.getRevokedAt()
        );
    }
    
}
