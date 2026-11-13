package com.beanstack.notifyx.health.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.health.dto.OperationalHealthResponse;
import com.beanstack.notifyx.health.service.OperationalHealthService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class OperationalHealthController {

    private final OperationalHealthService healthService;

    @GetMapping("/operations")
    public ResponseEntity<ApiResponse<OperationalHealthResponse>> operations() {
        return ResponseEntity.ok(ApiResponse.success(
            "Operational health retrieved successfully",
            healthService.getHealth()
        ));
    }
}
