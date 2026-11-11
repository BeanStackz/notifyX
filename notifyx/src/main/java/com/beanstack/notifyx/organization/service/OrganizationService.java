package com.beanstack.notifyx.organization.service;

import java.util.List;
import java.util.UUID;

import com.beanstack.notifyx.organization.dto.request.CreateOrganizationRequest;
import com.beanstack.notifyx.organization.dto.response.OrganizationResponse;

public interface OrganizationService {

    OrganizationResponse createOrganization(String userEmail,CreateOrganizationRequest request);

    List<OrganizationResponse> getMyOrganizations(String userEmail);

    OrganizationResponse getOrganization(UUID organizationId,String userEmail);
    
}
