package com.beanstack.notifyx.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(
    String keyId,
    String keySecret,
    String webhookSecret,
    String baseUrl
) {}