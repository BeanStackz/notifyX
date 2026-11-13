package com.beanstack.notifyx.outbox.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.beanstack.notifyx.outbox.config.RabbitTopologyConfig;
import com.beanstack.notifyx.outbox.entity.OutboxEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private final OutboxPersistenceService persistenceService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Set<String> returnedMessageIds =
        ConcurrentHashMap.newKeySet();

    private final String publisherId = UUID.randomUUID().toString();

    @PostConstruct
    void configureReturns() {
        rabbitTemplate.setReturnsCallback(returned ->
            returnedMessageIds.add(
                returned.getMessage().getMessageProperties().getMessageId()
            )
        );
    }

    @Scheduled(fixedDelayString = "${notifyx.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = persistenceService.claimEvents(publisherId);

        for (OutboxEvent event : events) {
            try {
                publish(event);
                persistenceService.markPublished(event.getId(), publisherId);
            } catch (Exception exception) {
                persistenceService.markFailed(
                    event.getId(), publisherId, exception
                );
            }
        }
    }

    private void publish(OutboxEvent event) throws Exception {
        int priority = priority(event.getPayload());
        CorrelationData correlation = new CorrelationData(event.getId().toString());
        Message message = MessageBuilder
            .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .setMessageId(event.getId().toString())
            .setHeader("eventType", event.getEventType())
            .setHeader("eventVersion", event.getEventVersion())
            .setPriority(priority)
            .build();

        rabbitTemplate.send(
            RabbitTopologyConfig.EXCHANGE,
            RabbitTopologyConfig.ROUTING_KEY,
            message,
            correlation
        );

        var confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
        if (!confirm.ack()) {
            throw new IllegalStateException(
                "RabbitMQ rejected outbox event: " + confirm.reason()
            );
        }
        if (returnedMessageIds.remove(event.getId().toString())) {
            throw new IllegalStateException(
                "RabbitMQ returned outbox event because it was unroutable"
            );
        }
    }


    private int priority(String payload) {
        try {
            String value = objectMapper.readTree(payload)
                .path("priority")
                .asString("LOW");
            return switch (value) {
                case "CRITICAL" -> 4;
                case "HIGH" -> 3;
                case "NORMAL" -> 2;
                default -> 1;
            };
        } catch (Exception exception) {
            return 1;
        }
    }

}
