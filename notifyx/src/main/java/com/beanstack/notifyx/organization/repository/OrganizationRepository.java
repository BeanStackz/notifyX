package com.beanstack.notifyx.organization.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.organization.entity.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
