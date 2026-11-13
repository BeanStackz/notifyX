package com.beanstack.notifyx.health.service;

import java.time.Instant;
import java.util.Properties;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

import com.beanstack.notifyx.delivery.config.BrevoProperties;
import com.beanstack.notifyx.health.dto.OperationalHealthResponse;
import com.beanstack.notifyx.outbox.config.RabbitTopologyConfig;
import com.beanstack.notifyx.outbox.entity.OutboxStatus;
import com.beanstack.notifyx.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationalHealthService {

    private final ConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final BrevoProperties brevoProperties;
    private final OutboxEventRepository outboxRepository;

    public OperationalHealthResponse getHealth() {
        String rabbitMqStatus = "DOWN";
        try (Connection connection = connectionFactory.createConnection()) {
            rabbitMqStatus = connection.isOpen() ? "UP" : "DOWN";
        } catch (RuntimeException ignored) {
            // Health is reported as DOWN while the rest of the authenticated API remains available.
        }

        long deadLetterQueueCount = 0;
        try {
            Properties properties = amqpAdmin.getQueueProperties(RabbitTopologyConfig.DLQ);
            if (properties != null) {
                Object count = properties.get("QUEUE_MESSAGE_COUNT");
                if (count instanceof Number number) deadLetterQueueCount = number.longValue();
            }
        } catch (RuntimeException ignored) {
            // A missing/unreachable queue is represented as zero with RabbitMQ marked separately.
        }

        long outboxBacklog = outboxRepository.countByStatus(OutboxStatus.PENDING)
            + outboxRepository.countByStatus(OutboxStatus.PROCESSING);
        String brevoStatus = isConfigured() ? "CONFIGURED" : "NOT_CONFIGURED";

        return new OperationalHealthResponse(
            rabbitMqStatus,
            brevoStatus,
            outboxBacklog,
            deadLetterQueueCount,
            Instant.now().toString()
        );
    }

    private boolean isConfigured() {
        return hasText(brevoProperties.apiKey())
            && hasText(brevoProperties.senderEmail())
            && hasText(brevoProperties.baseUrl());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
