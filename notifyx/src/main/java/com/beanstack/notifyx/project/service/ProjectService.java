package com.beanstack.notifyx.project.service;

import java.util.List;
import java.util.UUID;

import com.beanstack.notifyx.project.dto.request.CreateProjectRequest;
import com.beanstack.notifyx.project.dto.request.UpdateProjectRequest;
import com.beanstack.notifyx.project.dto.response.ProjectResponse;

public interface ProjectService {
    ProjectResponse createProject(
        UUID organizationId, 
        String userEmail, 
        CreateProjectRequest projectRequest
    );

    List<ProjectResponse> getOrganizationProjects(UUID organizationId, String userEmail);

    ProjectResponse getProject(
        UUID organizationId, 
        UUID projectId, 
        String userEmail
    );

    ProjectResponse updateProject(
        UUID organizationId, 
        UUID projectId, 
        String userEmail, 
        UpdateProjectRequest projectRequest
    );

    void archiveProject(
        UUID organizationId, 
        UUID projectId, 
        String userEmail
    );
}
