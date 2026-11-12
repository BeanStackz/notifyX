package com.beanstack.notifyx.subscription.dto.response;

import java.util.UUID;

public record SubscriptionCheckoutResponse(
    UUID localSubscriptionId,
    String razorpaySubscriptionId,
    String razorpayPlanId,
    String razorpayKeyId,
    String shortUrl,
    String planName,
    long amountInPaise,
    String currency
) {}