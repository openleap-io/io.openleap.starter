package io.openleap.starter.core.messaging.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class OutboxDispatcherConfig {

    @Bean
    @Profile("messaging-stub")
    public OutboxDispatcher loggingOutboxDispatcher() {
        return new LoggingOutboxDispatcher();
    }

    @Bean
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    public OutboxDispatcher rabbitMqOutboxDispatcher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${ol.starter.idempotency.messaging.outbox.dispatcher.confirmTimeoutMillis:5000}") long timeout) {
        return new RabbitMqOutboxDispatcher(rabbitTemplate, objectMapper, timeout);
    }

}
