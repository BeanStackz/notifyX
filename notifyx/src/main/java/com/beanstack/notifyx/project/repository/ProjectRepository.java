package com.beanstack.notifyx.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.project.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    List<Project> findAllByOrganizationId(UUID organizationId);

    Optional<Project> findByIdAndOrganizationId(
            UUID projectId,
            UUID organizationId
    );

    boolean existsByOrganizationIdAndSlug(
            UUID organizationId,
            String slug
    );

    boolean existsByOrganizationIdAndSlugAndIdNot(
            UUID organizationId,
            String slug,
            UUID projectId
    );
}
