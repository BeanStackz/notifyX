package com.beanstack.notifyx.organization.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.organization.dto.request.CreateOrganizationRequest;
import com.beanstack.notifyx.organization.dto.response.OrganizationResponse;
import com.beanstack.notifyx.organization.service.OrganizationService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
@RequestMapping ("/api/v1/organizations")
@Tag(name = "Organizations", description = "Manage organizations and retrieve organizations the current user belongs to.")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping 
    @Operation(summary = "Create organization", description = "Creates an organization and adds the authenticated user as its owner.")
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody CreateOrganizationRequest request) {

        OrganizationResponse organizationResponse = organizationService.createOrganization(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created successfully", organizationResponse));
    }

    @GetMapping 
    @Operation(summary = "List my organizations", description = "Lists organizations the authenticated user is a member of.")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getMyOrganizations(
        @AuthenticationPrincipal UserDetails userDetails) {

        List<OrganizationResponse> organizations = organizationService.getMyOrganizations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Organizations retrieved successfully", organizations));
    }

    @GetMapping ("/{organizationId}")
    @Operation(summary = "Get organization", description = "Returns an organization when the authenticated user is a member.")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable UUID organizationId) {

        OrganizationResponse organizationResponse = organizationService.getOrganization(organizationId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Organization retrieved successfully", organizationResponse));
    }


    
}
