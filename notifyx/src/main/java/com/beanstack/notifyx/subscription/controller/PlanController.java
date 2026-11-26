package com.beanstack.notifyx.subscription.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.shared.response.ApiResponse;
import com.beanstack.notifyx.subscription.dto.response.PlanResponse;
import com.beanstack.notifyx.subscription.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Available subscription plans, quotas, rates, and supported notification channels.")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "List plans", description = "Returns the available plans and their pricing, quotas, rate limits, priority, and allowed channels.")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Plans retrieved successfully",
                subscriptionService.getPlans()
            )
        );
    }
}