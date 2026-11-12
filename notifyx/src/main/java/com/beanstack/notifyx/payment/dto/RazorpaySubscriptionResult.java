package com.beanstack.notifyx.payment.dto;

public record RazorpaySubscriptionResult(
    String subscriptionId,
    String shortUrl,
    String status
) {}