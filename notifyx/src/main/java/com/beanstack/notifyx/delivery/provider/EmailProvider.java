package com.beanstack.notifyx.delivery.provider;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.beanstack.notifyx.delivery.config.BrevoProperties;
import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.entity.NotificationChannel;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "notifyx.email.provider",
    havingValue = "brevo"
)
public class EmailProvider implements NotificationProvider {

    private final BrevoProperties properties;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(Notification notification) {
        validateConfiguration();

        Map<String, Object> body = Map.of(
            "sender", Map.of(
                "email", properties.senderEmail(),
                "name", properties.senderName()
            ),
            "to", new Map[] {
                Map.of("email", notification.getRecipient())
            },
            "subject", subject(notification),
            "htmlContent", htmlContent(notification.getContent())
        );

        JsonNode response = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeader("api-key", properties.apiKey())
            .build()
            .post()
            .uri("/v3/smtp/email")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        String messageId = response == null
            ? ""
            : response.path("messageId").asString();

        return new DeliveryResult(
            messageId,
            messageId.isBlank() ? "Brevo accepted the message" : messageId
        );
    }

    private void validateConfiguration() {
        if (isBlank(properties.apiKey()) ||
            isBlank(properties.senderEmail()) ||
            isBlank(properties.baseUrl())) {
            throw new IllegalStateException(
                "Brevo email configuration is incomplete"
            );
        }
    }

    private String subject(Notification notification) {
        return notification.getSubject() == null ||
            notification.getSubject().isBlank()
            ? "NotifyX notification"
            : notification.getSubject();
    }

    private String htmlContent(String content) {
        return "<p>" + content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\r\n", "<br>")
            .replace("\n", "<br>") + "</p>";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
