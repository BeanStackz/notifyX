package com.beanstack.notifyx.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifySubscriptionPaymentRequest(
    @NotBlank String razorpayPaymentId,
    @NotBlank String razorpaySubscriptionId,
    @NotBlank String razorpaySignature
) {}