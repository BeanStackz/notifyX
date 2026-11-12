package com.beanstack.notifyx.payment.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.beanstack.notifyx.payment.service.RazorpayWebhookService;

@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> receive(
        @RequestBody byte[] rawBody,
        @RequestHeader("X-Razorpay-Signature") String signature,
        @RequestHeader("x-razorpay-event-id") String eventId
    ) {
        webhookService.process(rawBody, signature, eventId);
        return ResponseEntity.ok().build();
    }
}