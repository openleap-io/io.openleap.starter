package io.openleap.common;

import io.openleap.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "io.openleap.common")
@EnableJpaRepositories(basePackages = "io.openleap.common")
@EntityScan(basePackages = "io.openleap.common")
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

    @Bean
    public JsonMapper jsonMapper() {
        return new JsonMapper();
    }

}
