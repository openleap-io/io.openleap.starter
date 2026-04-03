package io.openleap.core.scheduling.messaging;

import io.openleap.core.messaging.event.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(EventPublisher.class)
@ConditionalOnProperty(name = "task.listeners.events.enabled", havingValue = "true")
public class TaskMessagingConfiguration {

    @Bean
    public TaskEventPublisher taskEventPublisher(EventPublisher eventPublisher,
                                                 @Value("${spring.application.name}") String appName) {
        return new TaskEventPublisher(eventPublisher, appName);
    }
}
