package com.beanstack.notifyx.organization.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.identity.entity.User;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.organization.dto.request.CreateOrganizationRequest;
import com.beanstack.notifyx.organization.dto.response.OrganizationResponse;
import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.organization.entity.OrganizationMember;
import com.beanstack.notifyx.organization.entity.OrganizationRole;
import com.beanstack.notifyx.organization.repository.OrganizationMemberRepository;
import com.beanstack.notifyx.organization.repository.OrganizationRepository;
import com.beanstack.notifyx.organization.service.OrganizationService;
import com.beanstack.notifyx.shared.exception.OrganizationSlugAlreadyExistsException;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;
import com.beanstack.notifyx.subscription.service.DefaultSubscriptionProvisioner;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    private final DefaultSubscriptionProvisioner subscriptionProvisioner;
    
    
    @Override
    @Transactional 
    public OrganizationResponse createOrganization(String userEmail,CreateOrganizationRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
                
        String slug = request.slug()
            .trim()
            .toLowerCase(Locale.ROOT);
        if(organizationRepository.existsBySlug(slug)) {
            throw new OrganizationSlugAlreadyExistsException(slug);
        }

        Organization organization = Organization.builder()
            .name(request.name().trim())
            .slug(slug)
            .enabled(true)
            .build();

        Organization savedOrganization = organizationRepository.save(organization);
        subscriptionProvisioner.assignFreePlan(savedOrganization);

        OrganizationMember organizationMember = OrganizationMember.builder()
            .organization(savedOrganization)
            .user(user)
            .role(OrganizationRole.OWNER)
            .build();
        organizationMemberRepository.save(organizationMember);
        return toResponse(savedOrganization, OrganizationRole.OWNER);
    }

    @Override
    @Transactional (readOnly = true)
    public List<OrganizationResponse> getMyOrganizations(String userEmail) {
        User user = userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return organizationMemberRepository.findAllByUserId(user.getId()).stream()
                .map(member -> toResponse(member.getOrganization(), member.getRole()))
                .toList();
    }

    @Override
    @Transactional (readOnly = true)
    public OrganizationResponse getOrganization(UUID organizationId, String userEmail) {
        
        User user = userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found or user is not a member of the organization"));
        
        return toResponse(member.getOrganization(), member.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
    
    private OrganizationResponse toResponse(Organization organization, OrganizationRole role) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getName(),
            organization.getSlug(),
            role,
            organization.getCreatedAt()
        );
    }
    
}
