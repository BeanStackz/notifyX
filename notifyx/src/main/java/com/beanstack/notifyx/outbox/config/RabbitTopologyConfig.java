package com.beanstack.notifyx.outbox.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "notifyx.notification.exchange";
    public static final String QUEUE = "notifyx.notification.queue";
    public static final String ROUTING_KEY = "notification";
    public static final String DLX = "notifyx.notification.dlx";
    public static final String DLQ = "notifyx.notification.dlq";
    public static final String DLQ_ROUTING_KEY = "notification.dead";

    @Bean
    DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
            .withArgument("x-max-priority", 4)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
            .to(notificationExchange())
            .with(ROUTING_KEY);
    }

    @Bean
    Binding notificationDeadLetterBinding() {
        return BindingBuilder.bind(notificationDeadLetterQueue())
            .to(notificationDeadLetterExchange())
            .with(DLQ_ROUTING_KEY);
    }

    @Bean
    RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        connectionFactory.setPublisherConfirmType(
            CachingConnectionFactory.ConfirmType.CORRELATED
        );
        connectionFactory.setPublisherReturns(true);

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }
}
