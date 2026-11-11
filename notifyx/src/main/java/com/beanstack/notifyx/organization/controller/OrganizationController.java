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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
@RequestMapping ("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping 
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody CreateOrganizationRequest request) {

        OrganizationResponse organizationResponse = organizationService.createOrganization(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created successfully", organizationResponse));
    }

    @GetMapping 
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getMyOrganizations(
        @AuthenticationPrincipal UserDetails userDetails) {

        List<OrganizationResponse> organizations = organizationService.getMyOrganizations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Organizations retrieved successfully", organizations));
    }

    @GetMapping ("/{organizationId}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable UUID organizationId) {

        OrganizationResponse organizationResponse = organizationService.getOrganization(organizationId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Organization retrieved successfully", organizationResponse));
    }


    
}
