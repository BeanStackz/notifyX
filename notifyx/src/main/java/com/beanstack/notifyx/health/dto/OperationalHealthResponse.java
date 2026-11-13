package com.beanstack.notifyx.health.dto;

public record OperationalHealthResponse(
    String rabbitMqStatus,
    String brevoStatus,
    long outboxBacklog,
    long deadLetterQueueCount,
    String checkedAt
) {}
