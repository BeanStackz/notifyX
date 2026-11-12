package com.beanstack.notifyx.project.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.identity.entity.User;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.organization.entity.OrganizationMember;
import com.beanstack.notifyx.organization.entity.OrganizationRole;
import com.beanstack.notifyx.organization.repository.OrganizationMemberRepository;
import com.beanstack.notifyx.organization.repository.OrganizationRepository;
import com.beanstack.notifyx.project.dto.request.CreateProjectRequest;
import com.beanstack.notifyx.project.dto.request.UpdateProjectRequest;
import com.beanstack.notifyx.project.dto.response.ProjectResponse;
import com.beanstack.notifyx.project.entity.Project;
import com.beanstack.notifyx.project.entity.ProjectStatus;
import com.beanstack.notifyx.project.repository.ProjectRepository;
import com.beanstack.notifyx.project.service.ProjectService;
import com.beanstack.notifyx.shared.exception.ProjectArchivedException;
import com.beanstack.notifyx.shared.exception.ProjectSlugAlreadyExistsException;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    
    @Override 
    @Transactional 
    public ProjectResponse createProject(UUID organizationId, String userEmail, CreateProjectRequest projectRequest) {
        
        User user = findUser(userEmail);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if(!organization.isEnabled()) {
            throw new ResourceNotFoundException("Organization is not enabled");
        }
        OrganizationMember membership = findMembership(organizationId, user.getId());

        requireManagementPermission(membership);

        String slug = projectRequest.slug();

        if(projectRepository.existsByOrganizationIdAndSlug(organizationId, slug)) {
            throw new ProjectSlugAlreadyExistsException("Project with the same slug already exists in the organization");
        }

        Project project = Project.builder()
            .organization(organization)
            .name(projectRequest.name().trim())
            .slug(normalizeSlug(slug))
            .description(normalizeDescription(projectRequest.description()))
            .status(ProjectStatus.ACTIVE)
            .build();
        try{
            project = projectRepository.save(project);
            return toResponse(project);
        } catch (DataIntegrityViolationException e) {
            throw new ProjectSlugAlreadyExistsException("Failed to create project" + e.getMessage());
        }
    }

    @Override
    @Transactional (readOnly = true)
    public List<ProjectResponse> getOrganizationProjects(UUID organizationId, String userEmail) {
        User user = findUser(userEmail);

        findMembership(organizationId, user.getId());
        return projectRepository.findAllByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional (readOnly = true)
    public ProjectResponse getProject(UUID organizationId, UUID projectId, String userEmail) {
        User user = findUser(userEmail);
        findMembership(organizationId, user.getId());
        Project project = findProject(organizationId, projectId);
        return toResponse(project);
    }

    @Override
    public ProjectResponse updateProject(UUID organizationId, UUID projectId, String userEmail,
            UpdateProjectRequest projectRequest) {
        User user = findUser(userEmail);
        OrganizationMember membership = findMembership(organizationId, user.getId());
        requireManagementPermission(membership);
        Project project = findProject(organizationId, projectId);
        if(project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ProjectArchivedException("Cannot update an archived project");
        }
        String slug = normalizeSlug(projectRequest.slug());

        boolean slugTaken = projectRepository.existsByOrganizationIdAndSlugAndIdNot(organizationId, slug, projectId);

        if(slugTaken) {
            throw new ProjectSlugAlreadyExistsException("Project with the same slug already exists in the organization");
        }
        project.setName(projectRequest.name().trim());
        project.setSlug(normalizeSlug(projectRequest.slug()));
        project.setDescription(normalizeDescription(projectRequest.description()));
        try {
            Project updatedProject = projectRepository.saveAndFlush(project);
            return toResponse(updatedProject);
        } catch (DataIntegrityViolationException e) {
            throw new ProjectSlugAlreadyExistsException("Failed to update project" + e.getMessage());
        }
    }

    @Override
    @Transactional 
    public void archiveProject(UUID organizationId, UUID projectId, String userEmail) {
    
        User user = findUser(userEmail);
        OrganizationMember membership = findMembership(organizationId, user.getId());
        requireManagementPermission(membership);
        Project project = findProject(organizationId, projectId);
        if(project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ProjectArchivedException("Project is already archived");
        }
        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    private OrganizationMember findMembership(UUID organizationId, UUID userId) {
        return organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of the organization"));
    }
    private void requireManagementPermission(OrganizationMember membership) {
        OrganizationRole role = membership.getRole();
        if(role != OrganizationRole.ADMIN && role != OrganizationRole.OWNER) {
            throw new AccessDeniedException("User does not have permission to manage projects");
        }
    }
    private Project findProject(UUID organizationId, UUID projectId) {
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganization().getId(),
                project.getName(),
                project.getSlug(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }
    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}
