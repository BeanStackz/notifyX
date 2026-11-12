package com.beanstack.notifyx.apikey.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.apikey.security.ApiKeyPrincipal;
import com.beanstack.notifyx.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/ingest")
public class ApiKeyTestController {

    @GetMapping("/whoami")
    public ResponseEntity<ApiResponse<ApiKeyPrincipal>> whoAmI(
        @AuthenticationPrincipal ApiKeyPrincipal principal
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "API key authenticated successfully",
                principal
            )
        );
    }
}