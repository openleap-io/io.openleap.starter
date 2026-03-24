package io.openleap.core;

import io.openleap.core.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Bean
    public Queue testQueue() {
        return new Queue(MessagingConstants.OUTBOX_QUEUE, false);
    }

    @Bean
    public Binding testBinding(Queue testQueue, TopicExchange eventsExchange) {
        return BindingBuilder
                .bind(testQueue)
                .to(eventsExchange)
                .with(MessagingConstants.OUTBOX_ROUTING_KEY);
    }
}