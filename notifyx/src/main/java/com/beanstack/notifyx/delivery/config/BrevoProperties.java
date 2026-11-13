package com.beanstack.notifyx.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifyx.email.brevo")
public record BrevoProperties(
    String apiKey,
    String senderEmail,
    String senderName,
    String baseUrl,
    String webhookSecret
) {}
