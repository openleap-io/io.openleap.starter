package io.openleap.core.scheduling.inmemory.config;

import io.openleap.core.scheduling.api.queue.TaskQueue;
import io.openleap.core.scheduling.inmemory.queue.InMemoryTaskQueue;
import io.openleap.core.scheduling.inmemory.step.DirectStepRunner;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(name = "task.executor", havingValue = "in-memory")
@EnableConfigurationProperties(InMemoryTaskProperties.class)
public class InMemoryTaskConfiguration {

    @Bean
    DirectStepRunner directStepRunner() {
        return new DirectStepRunner();
    }

    @Bean
    TaskQueue inMemoryTaskQueue(TaskHandlerRegistry registry,
                                DirectStepRunner directStepRunner,
                                CompositeTaskLifecycleListener listener,
                                InMemoryTaskProperties properties,
                                JsonMapper jsonMapper) {
        return new InMemoryTaskQueue(registry, properties.getExecutorType().create(properties.getThreadPoolSize()), directStepRunner, listener, jsonMapper);
    }
}
