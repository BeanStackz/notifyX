package com.beanstack.notifyx.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.payment.entity.Payment;
import com.beanstack.notifyx.payment.entity.PaymentStatus;
import com.beanstack.notifyx.payment.entity.WebhookProcessingStatus;
import com.beanstack.notifyx.payment.repository.PaymentRepository;
import com.beanstack.notifyx.payment.repository.RazorpayWebhookEventRepository;
import com.beanstack.notifyx.subscription.entity.OrganizationSubscription;
import com.beanstack.notifyx.subscription.entity.RazorpayWebhookEvent;
import com.beanstack.notifyx.subscription.entity.SubscriptionStatus;
import com.beanstack.notifyx.subscription.entity.SubscriptionUsage;
import com.beanstack.notifyx.subscription.repository.OrganizationSubscriptionRepository;
import com.beanstack.notifyx.subscription.repository.SubscriptionUsageRepository;
import com.beanstack.notifyx.subscription.service.DefaultSubscriptionProvisioner;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final ObjectMapper objectMapper;
    private final RazorpayClientService razorpayClientService;

    private final RazorpayWebhookEventRepository webhookRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionUsageRepository usageRepository;
    private final PaymentRepository paymentRepository;

    private final DefaultSubscriptionProvisioner freeProvisioner;

    @Transactional
    public void process(
        byte[] rawBody,
        String signature,
        String eventId
    ) {
        verifySignature(rawBody, signature);

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                "Razorpay event ID is missing"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            String eventType = root.path("event").asText();

            if (eventType.isBlank()) {
                throw new IllegalArgumentException(
                    "Razorpay event type is missing"
                );
            }

            int inserted = webhookRepository.insertIfAbsent(
                UUID.randomUUID(),
                eventId,
                eventType,
                new String(rawBody, StandardCharsets.UTF_8),
                Instant.now()
            );

            if (inserted == 0) {
                return;
            }

            processSubscriptionEvent(root, eventType);

            RazorpayWebhookEvent webhook =
                webhookRepository.findByEventId(eventId)
                    .orElseThrow();

            webhook.setStatus(WebhookProcessingStatus.PROCESSED);
            webhook.setProcessedAt(Instant.now());

        } catch (Exception exception) {
            throw new IllegalStateException(
                "Razorpay webhook processing failed",
                exception
            );
        }
    }

    private void processSubscriptionEvent(
        JsonNode root,
        String eventType
    ) {
        JsonNode subscriptionEntity = root
            .path("payload")
            .path("subscription")
            .path("entity");

        String razorpaySubscriptionId =
            subscriptionEntity.path("id").asText();

        if (razorpaySubscriptionId.isBlank()) {
            return;
        }

        OrganizationSubscription subscription =
            subscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Unknown Razorpay subscription"
                    )
                );

        switch (eventType) {
            case "subscription.activated" -> activatePaidSubscription(
                subscription,
                subscriptionEntity
            );

            case "subscription.charged" -> {
                activatePaidSubscription(subscription, subscriptionEntity);
                recordPayment(root, subscription);
            }

            case "subscription.pending" ->
                subscription.setStatus(SubscriptionStatus.PAST_DUE);

            case "subscription.halted" -> {
                subscription.setStatus(SubscriptionStatus.HALTED);
                fallbackToFree(subscription);
            }

            case "subscription.cancelled" -> {
                Instant periodEnd = subscription.getCurrentPeriodEnd();

                if (periodEnd != null && periodEnd.isAfter(Instant.now())) {
                    subscription.setCancelAtPeriodEnd(true);
                } else {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    fallbackToFree(subscription);
                }
            }

            case "subscription.completed" -> {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                fallbackToFree(subscription);
            }

            default -> {
                // Store but do not change state for unsupported events.
            }
        }
    }

    private void activatePaidSubscription(
        OrganizationSubscription subscription,
        JsonNode razorpayEntity
    ) {
        Instant start = epochToInstant(
            razorpayEntity.path("current_start").asLong()
        );

        Instant end = epochToInstant(
            razorpayEntity.path("current_end").asLong()
        );

        if (start == null) {
            start = Instant.now();
        }

        if (end == null) {
            end = start.atZone(ZoneOffset.UTC)
                .plusMonths(1)
                .toInstant();
        }

        // Retire the old FREE or paid subscription before activating this one.
        var existingActive = subscriptionRepository
            .findFirstByOrganization_IdAndStatusOrderByCreatedAtDesc(
                subscription.getOrganization().getId(),
                SubscriptionStatus.ACTIVE
            );

        if (existingActive.isPresent() &&
            !existingActive.get().getId().equals(subscription.getId())) {
            existingActive.get().setStatus(SubscriptionStatus.EXPIRED);
        }

        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(end);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        initializeUsage(subscription, start, end);
    }

    private void initializeUsage(
        OrganizationSubscription subscription,
        Instant start,
        Instant end
    ) {
        boolean exists = usageRepository
            .findBySubscription_IdAndPeriodStart(subscription.getId(), start)
            .isPresent();

        if (!exists) {
            usageRepository.save(
                SubscriptionUsage.builder()
                    .subscription(subscription)
                    .periodStart(start)
                    .periodEnd(end)
                    .notificationsUsed(0)
                    .build()
            );
        }
    }

    private void recordPayment(
        JsonNode root,
        OrganizationSubscription subscription
    ) {
        JsonNode payment = root
            .path("payload")
            .path("payment")
            .path("entity");

        String paymentId = payment.path("id").asText();

        if (paymentId.isBlank() ||
            paymentRepository.existsByRazorpayPaymentId(paymentId)) {
            return;
        }

        paymentRepository.save(
            Payment.builder()
                .subscription(subscription)
                .razorpayPaymentId(paymentId)
                .amountInPaise(payment.path("amount").asLong())
                .currency(payment.path("currency").asText("INR"))
                .status(PaymentStatus.CAPTURED)
                .build()
        );
    }

    private void fallbackToFree(
        OrganizationSubscription subscription
    ) {
        Organization organization = subscription.getOrganization();

        var active = subscriptionRepository
            .findFirstByOrganization_IdAndStatusOrderByCreatedAtDesc(
                organization.getId(),
                SubscriptionStatus.ACTIVE
            );

        if (active.isPresent() &&
            !active.get().getId().equals(subscription.getId())) {
            active.get().setStatus(SubscriptionStatus.EXPIRED);
        }

        freeProvisioner.activateFreePlan(organization);
    }

    private Instant epochToInstant(long value) {
        return value <= 0 ? null : Instant.ofEpochSecond(value);
    }

    private void verifySignature(
        byte[] rawBody,
        String receivedSignature
    ) {
        if (receivedSignature == null ||
            !receivedSignature.matches("[a-fA-F0-9]{64}")) {
            throw new IllegalArgumentException(
                "Invalid Razorpay webhook signature"
            );
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                razorpayClientService.getWebhookSecret()
                    .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            ));

            byte[] expected = mac.doFinal(rawBody);
            byte[] received = HexFormat.of().parseHex(receivedSignature);

            if (!MessageDigest.isEqual(expected, received)) {
                throw new IllegalArgumentException(
                    "Razorpay webhook signature verification failed"
                );
            }

        } catch (IllegalArgumentException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalStateException(
                "Webhook signature verification failed",
                exception
            );
        }
    }
}