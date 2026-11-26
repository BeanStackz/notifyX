package com.beanstack.notifyx.project.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.project.dto.request.CreateProjectRequest;
import com.beanstack.notifyx.project.dto.request.UpdateProjectRequest;
import com.beanstack.notifyx.project.dto.response.ProjectResponse;
import com.beanstack.notifyx.project.service.ProjectService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/organizations/{organizationId}/projects")
@RequiredArgsConstructor 
@Tag(name = "Projects", description = "Create, discover, update, and archive organization-scoped projects.")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping 
    @Operation(summary = "Create project", description = "Creates a project in the organization. Requires organization owner or administrator privileges.")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@PathVariable UUID organizationId, @RequestBody CreateProjectRequest projectRequest, @AuthenticationPrincipal UserDetails userDetails) {
        ProjectResponse projectResponse = projectService.createProject(organizationId, userDetails.getUsername(), projectRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Project created successfully", projectResponse));
    }

    @GetMapping 
    @Operation(summary = "List projects", description = "Lists all projects in an organization the authenticated user belongs to.")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(
        @PathVariable UUID organizationId, 
        @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectResponse> projects = projectService.getOrganizationProjects(organizationId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Projects retrieved successfully", projects));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project", description = "Returns a project in the specified organization.")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
        @PathVariable UUID organizationId, 
        @PathVariable UUID projectId, 
        @AuthenticationPrincipal UserDetails userDetails) {
        ProjectResponse project = projectService.getProject(organizationId, projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Project retrieved successfully", project));
    }

    @PutMapping ("/{projectId}")
    @Operation(summary = "Update project", description = "Replaces the project's name, slug, and optional description. Requires organization owner or administrator privileges.")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
        @PathVariable UUID organizationId, 
        @PathVariable UUID projectId, 
        @Valid @RequestBody UpdateProjectRequest projectRequest, 
        @AuthenticationPrincipal UserDetails userDetails) {
        ProjectResponse updatedProject = projectService.updateProject(organizationId, projectId, userDetails.getUsername(), projectRequest);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", updatedProject));
    }

    @PatchMapping ("/{projectId}/archive")
    @Operation(summary = "Archive project", description = "Archives a project. Requires organization owner or administrator privileges; archived projects cannot be reactivated through this API.")
    public ResponseEntity<ApiResponse<Void>> archiveProject(
        @PathVariable UUID organizationId, 
        @PathVariable UUID projectId, 
        @AuthenticationPrincipal UserDetails userDetails) {
        projectService.archiveProject(organizationId, projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Project archived successfully", null));
    }

}