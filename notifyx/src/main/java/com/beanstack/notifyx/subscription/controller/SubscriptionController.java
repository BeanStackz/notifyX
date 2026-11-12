package com.beanstack.notifyx.subscription.controller;

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

import com.beanstack.notifyx.shared.response.ApiResponse;
import com.beanstack.notifyx.subscription.dto.request.SelectPlanRequest;
import com.beanstack.notifyx.subscription.dto.request.VerifySubscriptionPaymentRequest;
import com.beanstack.notifyx.subscription.dto.response.SubscriptionCheckoutResponse;
import com.beanstack.notifyx.subscription.dto.response.SubscriptionResponse;
import com.beanstack.notifyx.subscription.service.SubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
        @PathVariable UUID organizationId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "Subscription retrieved successfully",
                subscriptionService.getCurrentSubscription(
                    organizationId,
                    userDetails.getUsername()
                )
            )
        );
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<SubscriptionCheckoutResponse>> checkout(
        @PathVariable UUID organizationId,
        @Valid @RequestBody SelectPlanRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        SubscriptionCheckoutResponse response =
            subscriptionService.createCheckout(
                organizationId,
                userDetails.getUsername(),
                request.planCode()
            );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "Razorpay checkout created successfully",
                    response
                )
            );
    }

    @PostMapping("/{subscriptionId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCheckout(
        @PathVariable UUID organizationId,
        @PathVariable UUID subscriptionId,
        @Valid @RequestBody VerifySubscriptionPaymentRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        subscriptionService.verifyCheckout(
            organizationId,
            subscriptionId,
            userDetails.getUsername(),
            request
        );

        return ResponseEntity.ok(
            ApiResponse.success(
                "Checkout signature verified. Waiting for subscription activation.",
                null
            )
        );
    }
}