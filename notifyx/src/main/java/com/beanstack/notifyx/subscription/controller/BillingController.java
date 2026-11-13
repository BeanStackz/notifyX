package com.beanstack.notifyx.subscription.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.beanstack.notifyx.shared.response.ApiResponse;
import com.beanstack.notifyx.subscription.dto.request.SelectPlanRequest;
import com.beanstack.notifyx.subscription.dto.request.VerifyPaymentRequest;
import com.beanstack.notifyx.subscription.dto.response.BillingCheckoutResponse;
import com.beanstack.notifyx.subscription.service.SubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/billing")
@RequiredArgsConstructor
public class BillingController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<BillingCheckoutResponse>> checkout(
        @PathVariable UUID organizationId,
        @Valid @RequestBody SelectPlanRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success("Billing order created",
                subscriptionService.createBillingOrder(
                    organizationId, userDetails.getUsername(), request.planCode()
                ))
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
        @PathVariable UUID organizationId,
        @Valid @RequestBody VerifyPaymentRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        subscriptionService.verifyPayment(
            organizationId, userDetails.getUsername(), request
        );
        return ResponseEntity.ok(ApiResponse.success("Payment verified", null));
    }
}
