package com.beanstack.notifyx.authorization.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.shared.response.ApiResponse;

@RestController 
@RequestMapping ("/api/v1/admin")
public class AdminController {

    @GetMapping ("/dashboard")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> dashboard(){
        return ResponseEntity.ok(
            ApiResponse.success(
                "Welcome to the Admin Dashboard", 
                Map.of("module", "NotifyX", "access", "ADMIN"))
        );
    }
    
}
