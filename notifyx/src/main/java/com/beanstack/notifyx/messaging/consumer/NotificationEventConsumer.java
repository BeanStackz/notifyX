package com.beanstack.notifyx.messaging.consumer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.beanstack.notifyx.delivery.entity.DeliveryAttempt;
import com.beanstack.notifyx.delivery.service.DeliveryRetryService;
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
    private final DeliveryRetryService deliveryRetryService;

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
            var context = deliveryService.createContext(attempt);
            var provider = deliveryService.providerFor(context.channel());
            var result = provider.send(context);
            deliveryService.complete(
                attempt,
                result.providerMessageId(),
                result.providerResponse()
            );
            channel.basicAck(tag, false);
        } catch (Exception exception) {
            deliveryRetryService.handleFailure(attempt.getId(), exception);
            channel.basicAck(tag, false);
        }
    }
}
