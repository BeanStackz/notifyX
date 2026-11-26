package com.beanstack.notifyx.payment.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.beanstack.notifyx.payment.service.RazorpayWebhookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
@RequiredArgsConstructor
@Tag(name = "Provider webhooks", description = "Provider callbacks authenticated with provider-specific secrets or signatures.")
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    @PostMapping
    @Operation(
        summary = "Receive Razorpay webhook",
        description = "Processes a raw Razorpay event after verifying its signature and provider event ID."
    )
    public ResponseEntity<Void> receive(
        @RequestBody byte[] rawBody,
        @RequestHeader("X-Razorpay-Signature") String signature,
        @RequestHeader("x-razorpay-event-id") String eventId
    ) {
        webhookService.process(rawBody, signature, eventId);
        return ResponseEntity.ok().build();
    }
}