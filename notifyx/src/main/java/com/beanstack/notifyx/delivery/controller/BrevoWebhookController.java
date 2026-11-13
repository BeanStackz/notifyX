package com.beanstack.notifyx.delivery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.beanstack.notifyx.delivery.config.BrevoProperties;
import com.beanstack.notifyx.delivery.service.BrevoWebhookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/webhooks/brevo")
@RequiredArgsConstructor
public class BrevoWebhookController {
    private final BrevoWebhookService webhookService;
    private final BrevoProperties properties;

    @PostMapping
    public ResponseEntity<Void> receive(
        @RequestBody byte[] rawBody,
        @RequestHeader(value = "X-Brevo-Webhook-Key", required = false)
        String webhookKey,
        @RequestHeader(value = "Authorization", required = false)
        String authorization
    ) {
        String token = webhookKey;
        if ((token == null || token.isBlank()) &&
            authorization != null &&
            authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7).trim();
        }

        if (properties.webhookSecret() == null ||
            properties.webhookSecret().isBlank() ||
            token == null ||
            !MessageDigest.isEqual(
                properties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
            )) {
            return ResponseEntity.status(401).build();
        }
        webhookService.process(rawBody);
        return ResponseEntity.ok().build();
    }
}
