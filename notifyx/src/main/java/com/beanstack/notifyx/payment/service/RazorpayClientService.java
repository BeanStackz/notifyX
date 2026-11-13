package com.beanstack.notifyx.payment.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.beanstack.notifyx.payment.config.RazorpayProperties;
import com.beanstack.notifyx.payment.dto.RazorpaySubscriptionResult;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class RazorpayClientService {

    private final RazorpayProperties properties;

    private RestClient client() {
        if (properties.keyId() == null ||
            properties.keyId().isBlank() ||
            properties.keySecret() == null ||
            properties.keySecret().isBlank()) {
            throw new IllegalStateException(
                "Razorpay credentials are not configured"
            );
        }

        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeaders(headers ->
                headers.setBasicAuth(
                    properties.keyId(),
                    properties.keySecret()
                )
            )
            .build();
    }

    public String createPlan(
        String name,
        String description,
        long amountInPaise
    ) {
        Map<String, Object> item = Map.of(
            "name", name,
            "description", description,
            "amount", amountInPaise,
            "currency", "INR"
        );

        Map<String, Object> body = Map.of(
            "period", "monthly",
            "interval", 1,
            "item", item
        );

        JsonNode response = client().post()
            .uri("/v1/plans")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        String planId = response == null ? null : response.path("id").asString();

        if (planId == null || planId.isBlank()) {
            throw new IllegalStateException(
                "Razorpay did not return a plan ID"
            );
        }

        return planId;
    }

    public RazorpaySubscriptionResult createSubscription(
        String razorpayPlanId,
        String localSubscriptionId,
        String organizationId,
        int totalCount
    ) {
        Map<String, Object> notes = Map.of(
            "notifyx_subscription_id", localSubscriptionId,
            "notifyx_organization_id", organizationId
        );

        Map<String, Object> body = Map.of(
            "plan_id", razorpayPlanId,
            "total_count", totalCount,
            "quantity", 1,
            "customer_notify", true,
            "notes", notes
        );

        JsonNode response = client().post()
            .uri("/v1/subscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        if (response == null || response.path("id").asString().isBlank()) {
            throw new IllegalStateException(
                "Razorpay did not return a subscription ID"
            );
        }

        return new RazorpaySubscriptionResult(
            response.path("id").asString(),
            response.path("short_url").asString(null),
            response.path("status").asString()
        );
    }

    public String createOrder(
        long amountInPaise,
        String currency,
        UUID localOrderId,
        UUID organizationId
    ) {
        Map<String, Object> body = Map.of(
            "amount", amountInPaise,
            "currency", currency,
            "receipt", localOrderId.toString(),
            "notes", Map.of(
                "notifyx_order_id", localOrderId.toString(),
                "notifyx_organization_id", organizationId.toString()
            )
        );

        JsonNode response = client().post()
            .uri("/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        String orderId = response == null ? "" : response.path("id").asString();
        if (orderId.isBlank()) {
            throw new IllegalStateException("Razorpay did not return an order ID");
        }
        return orderId;
    }

    public JsonNode cancelSubscription(
        String razorpaySubscriptionId,
        boolean cancelAtCycleEnd
    ) {
        Map<String, Object> body = Map.of(
            "cancel_at_cycle_end",
            cancelAtCycleEnd
        );

        return client().post()
            .uri(
                "/v1/subscriptions/{id}/cancel",
                razorpaySubscriptionId
            )
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
    }

    public String getKeyId() {
        return properties.keyId();
    }

    public String getWebhookSecret() {
        return properties.webhookSecret();
    }
}