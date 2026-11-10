package com.beanstack.notifyx.identity.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.shared.response.ApiResponse;

@RestController 
@RequestMapping ("/api/v1/users")
public class UserController {
    
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> profile() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Profile accessed successfully",
                        Map.of("access", "USER")
                )
        );
    }
}
