package com.beanstack.notifyx.health.service;

import java.time.Instant;
import java.util.Properties;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.dao.DataAccessException;
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
        } catch (AmqpException exception) {
            rabbitMqStatus = "DOWN";
        }

        long deadLetterQueueCount = 0;
        try {
            Properties properties = amqpAdmin.getQueueProperties(RabbitTopologyConfig.DLQ);
            if (properties != null) {
                Object count = properties.get("QUEUE_MESSAGE_COUNT");
                if (count instanceof Number number) deadLetterQueueCount = number.longValue();
            }
        } catch (AmqpException exception) {
            deadLetterQueueCount = -1;
        }

        long outboxBacklog;
        try {
            outboxBacklog = outboxRepository.countByStatus(OutboxStatus.PENDING)
                + outboxRepository.countByStatus(OutboxStatus.PROCESSING);
        } catch (DataAccessException exception) {
            outboxBacklog = -1;
        }
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
