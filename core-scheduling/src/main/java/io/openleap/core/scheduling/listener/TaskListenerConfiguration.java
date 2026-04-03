package io.openleap.core.scheduling.listener;

import io.micrometer.core.instrument.MeterRegistry;
import io.openleap.core.scheduling.api.listener.TaskLifecycleListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TaskListenerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "task.listeners.logging.enabled", havingValue = "true")
    public TaskLoggingListener taskLoggingListener() {
        return new TaskLoggingListener();
    }

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(name = "task.listeners.metrics.enabled", havingValue = "true")
    public TaskMetricsListener taskMetricsListener(MeterRegistry meterRegistry) {
        return new TaskMetricsListener(meterRegistry);
    }

    @Bean
    public CompositeTaskLifecycleListener aggregateTaskLifecycleListener(List<TaskLifecycleListener> listeners) {
        return new CompositeTaskLifecycleListener(listeners);
    }
}
