package io.openleap.core.scheduling.config;

import io.openleap.core.scheduling.api.handler.RetryOptions;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.dbos.config.DbosTaskConfiguration;
import io.openleap.core.scheduling.iam.TaskIamConfiguration;
import io.openleap.core.scheduling.inmemory.config.InMemoryTaskConfiguration;

import io.openleap.core.scheduling.listener.TaskListenerConfiguration;
import io.openleap.core.scheduling.messaging.TaskMessagingConfiguration;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import io.openleap.core.scheduling.web.config.TaskWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

// TODO (itaseski): Consider adding ol. prefix to properties names to make them consistent with the other modules
@AutoConfiguration
@ConditionalOnProperty(prefix = "scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
@EnableConfigurationProperties(TaskRetryProperties.class)
@Import({DbosTaskConfiguration.class, InMemoryTaskConfiguration.class, TaskWebAutoConfiguration.class, TaskMessagingConfiguration.class, TaskListenerConfiguration.class, TaskIamConfiguration.class})
public class TaskAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    public TaskHandlerRegistry taskHandlerRegistry(List<TaskHandler<?, ?>> handlers) {
        return new TaskHandlerRegistry(handlers);
    }

    @Bean
    public RetryOptions defaultRetryOptions(TaskRetryProperties properties) {
        return new RetryOptions(properties.getMaxAttempts(), properties.getIntervalSeconds(), properties.getBackoffRate());
    }
}
