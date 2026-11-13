package com.beanstack.notifyx.delivery.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.delivery.repository.DeliveryAttemptRepository;
import com.beanstack.notifyx.delivery.repository.DeliveryEventRepository;
import com.beanstack.notifyx.notification.entity.NotificationStatus;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class BrevoWebhookService {
    private final DeliveryAttemptRepository attemptRepository;
    private final DeliveryEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(byte[] rawBody) {
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String eventType = text(payload, "event");
            String messageId = text(payload, "message-id");
            if (isBlank(eventType) || isBlank(messageId)) {
                return;
            }

            var attempt = attemptRepository
                .findFirstByProviderMessageId(messageId)
                .orElse(null);
            if (attempt == null) {
                return;
            }

            Instant occurredAt = Instant.now();
            String timestamp = text(payload, "ts_event");
            if (!isBlank(timestamp)) {
                try {
                    occurredAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
                } catch (NumberFormatException ignored) {
                    // Brevo can send malformed timestamps; receipt time remains safe.
                }
            }

            Instant receivedAt = Instant.now();
            int inserted = eventRepository.insertIfAbsent(
                java.util.UUID.randomUUID(),
                attempt.getNotification().getId(),
                messageId,
                eventType,
                text(payload, "email"),
                occurredAt,
                receivedAt
            );
            if (inserted == 1 && isFailureEvent(eventType)) {
                attempt.getNotification().setStatus(NotificationStatus.FAILED);
            }
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid Brevo webhook payload", exception);
        }
    }

    private boolean isFailureEvent(String eventType) {
        String normalized = eventType.toLowerCase().replace("_", "");
        return "hardbounce".equals(normalized) ||
            "softbounce".equals(normalized) ||
            "blocked".equals(normalized) ||
            "spam".equals(normalized) ||
            "invalid".equals(normalized);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
