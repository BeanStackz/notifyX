package com.beanstack.notifyx.messaging.consumer;

import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.beanstack.notifyx.delivery.entity.DeliveryAttempt;
import com.beanstack.notifyx.delivery.service.NotificationDeliveryService;
import com.beanstack.notifyx.outbox.config.RabbitTopologyConfig;
import com.beanstack.notifyx.outbox.dto.NotificationAcceptedEvent;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final ObjectMapper objectMapper;
    private final NotificationDeliveryService deliveryService;

    @RabbitListener(
        queues = RabbitTopologyConfig.QUEUE,
        containerFactory = "notificationListenerFactory"
    )
    public void consume(
        Message message,
        Channel channel
    ) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        String eventId = message.getMessageProperties().getMessageId();
        NotificationAcceptedEvent event = objectMapper.readValue(
            message.getBody(),
            NotificationAcceptedEvent.class
        );

        DeliveryAttempt attempt = deliveryService.start(
            event.notificationId(),
            eventId
        );
        if (attempt == null) {
            channel.basicAck(tag, false);
            return;
        }

        try {
            var notification = attempt.getNotification();
            var provider = deliveryService.providerFor(notification);
            var result = provider.send(notification);
            deliveryService.complete(
                attempt,
                result.providerMessageId(),
                result.providerResponse()
            );
            channel.basicAck(tag, false);
        } catch (Exception exception) {
            deliveryService.fail(attempt, exception);
            channel.basicNack(tag, false, false);
        }
    }
}
