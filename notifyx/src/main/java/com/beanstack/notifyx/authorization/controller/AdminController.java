package com.beanstack.notifyx.authorization.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.shared.response.ApiResponse;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.organization.repository.OrganizationRepository;
import com.beanstack.notifyx.project.repository.ProjectRepository;
import com.beanstack.notifyx.notification.repository.NotificationRepository;
import com.beanstack.notifyx.authorization.dto.AdminDashboardResponse;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping ("/dashboard")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard(){
        return ResponseEntity.ok(
            ApiResponse.success(
                "Admin dashboard retrieved successfully",
                new AdminDashboardResponse(
                    userRepository.count(),
                    organizationRepository.count(),
                    projectRepository.count(),
                    notificationRepository.count()
                ))
        );
    }
    
}
