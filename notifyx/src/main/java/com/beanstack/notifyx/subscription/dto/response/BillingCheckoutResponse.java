package com.beanstack.notifyx.subscription.dto.response;

import java.util.UUID;

public record BillingCheckoutResponse(
    UUID billingOrderId,
    String razorpayOrderId,
    String razorpayKeyId,
    long amountInPaise,
    String currency,
    String planName
) {}
