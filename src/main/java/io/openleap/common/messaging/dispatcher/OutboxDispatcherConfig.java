package io.openleap.common.messaging.dispatcher;

import io.openleap.common.messaging.dispatcher.logger.LoggingOutboxDispatcher;
import io.openleap.common.messaging.dispatcher.rabbitmq.RabbitMqOutboxDispatcher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class OutboxDispatcherConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ol.messaging.outbox.dispatcher", name = "type", havingValue = "logger")
    public OutboxDispatcher loggingOutboxDispatcher() {
        return new LoggingOutboxDispatcher();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ol.messaging.outbox.dispatcher", name = "type", havingValue = "rabbitmq")
    public OutboxDispatcher rabbitMqOutboxDispatcher(
            RabbitTemplate rabbitTemplate,
            JsonMapper jsonMapper,
            @Value("${ol.messaging.outbox.dispatcher.confirm-timeout-millis:5000}") long timeout) {
        return new RabbitMqOutboxDispatcher(rabbitTemplate, jsonMapper, timeout);
    }

}
