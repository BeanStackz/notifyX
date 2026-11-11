package com.beanstack.notifyx.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.organization.entity.OrganizationMember;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMember> findAllByUserId(UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(
        UUID organizationId, UUID userId);
    
}
