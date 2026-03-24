package io.openleap.core.messaging.config;

import io.openleap.core.messaging.MessageCoverageTracker;
import io.openleap.core.messaging.command.SimpleCommandBus;
import io.openleap.core.messaging.config.registrar.MessagingEntityRegistrar;
import io.openleap.core.messaging.config.registrar.MessagingRepositoryRegistrar;
import io.openleap.core.messaging.dispatcher.OutboxDispatcher;
import io.openleap.core.messaging.dispatcher.OutboxDispatcherConfig;
import io.openleap.core.messaging.event.EventPublisher;
import io.openleap.core.messaging.repository.OutboxRepository;
import io.openleap.core.messaging.service.MetricsService;
import io.openleap.core.messaging.service.OutboxAdminService;
import io.openleap.core.messaging.service.OutboxOrchestrator;
import io.openleap.core.messaging.service.OutboxProcessor;
import io.openleap.core.security.config.SecurityProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MessagingProperties.class)
@Import({AmqpConfig.class, OutboxDispatcherConfig.class, MessagingEntityRegistrar.class, MessagingRepositoryRegistrar.class})
public class MessagingAutoConfiguration {

    @Bean
    public MessageCoverageTracker messageCoverageTracker() {
        return new MessageCoverageTracker();
    }

    @Bean
    public SimpleCommandBus simpleCommandBus(ApplicationContext applicationContext) {
        return new SimpleCommandBus(applicationContext);
    }

    @Bean
    public MessagingIdentityPostProcessor messagingIdentityPostProcessor(Optional<SecurityProperties> securityProperties) {
        return new MessagingIdentityPostProcessor(securityProperties);
    }

    @Bean
    public OutboxProcessor outboxProcessor(OutboxRepository outboxRepository, OutboxDispatcher outboxDispatcher) {
        return new OutboxProcessor(outboxRepository, outboxDispatcher);
    }

    @Bean
    public OutboxOrchestrator outboxOrchestrator(OutboxProcessor outboxProcessor) {
        return new OutboxOrchestrator(outboxProcessor);
    }

    @Bean
    public EventPublisher eventPublisher(MessagingProperties config,
                                         OutboxRepository outboxRepository,
                                         JsonMapper jsonMapper,
                                         OutboxOrchestrator outboxOrchestrator,
                                         Optional<MessageCoverageTracker> coverageTracker) {
        return new EventPublisher(config, outboxRepository, jsonMapper, outboxOrchestrator, coverageTracker);
    }

    @Bean
    public MetricsService metricsService(MessagingProperties config,
                                         OutboxRepository outboxRepository,
                                         RabbitTemplate rabbitTemplate) {
        return new MetricsService(config, outboxRepository, rabbitTemplate);
    }

    @Bean
    public OutboxAdminService outboxAdminService(OutboxRepository outboxRepository) {
        return new OutboxAdminService(outboxRepository);
    }

}