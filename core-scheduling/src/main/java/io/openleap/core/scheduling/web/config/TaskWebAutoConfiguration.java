package io.openleap.core.scheduling.web.config;

import io.openleap.core.scheduling.api.queue.TaskQueue;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import io.openleap.core.scheduling.web.controller.TaskController;
import io.openleap.core.scheduling.web.error.TaskExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ConditionalOnProperty(name = "task.web.enabled", havingValue = "true", matchIfMissing = true)
// TODO (itaseski): TaskController is annotaed with @RestController and we need to protect it from
//  component scanning and only make it available if the user enables the web layer
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TaskController.class))
public class TaskWebAutoConfiguration {

    @Bean
    public TaskController taskController(TaskQueue taskQueue, TaskHandlerRegistry registry) {
        return new TaskController(taskQueue, registry);
    }

    @Bean
    public TaskExceptionHandler taskExceptionHandler() {
        return new TaskExceptionHandler();
    }
}