package io.openleap.core.scheduling.dbos.config;

import dev.dbos.transact.DBOS;
import dev.dbos.transact.config.DBOSConfig;
import dev.dbos.transact.workflow.Queue;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.api.queue.TaskQueue;
import io.openleap.core.scheduling.dbos.queue.DbosMapper;
import io.openleap.core.scheduling.dbos.queue.DbosTaskQueue;
import io.openleap.core.scheduling.dbos.step.DbosStepRunner;
import io.openleap.core.scheduling.dbos.workflow.TaskDispatchWorkflow;
import io.openleap.core.scheduling.dbos.workflow.TaskDispatchWorkflowImpl;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

// TODO (itaseski): Check DBOS claimed load rates
// TODO (itaseski): Add support for retention policies. Cleanup can probably be done using
//  @Scheduled and invoking the DBOS /gc API since there is no SDK config support for this
@Configuration
@ConditionalOnProperty(name = "task.executor", havingValue = "dbos", matchIfMissing = true)
@EnableConfigurationProperties({DbosProperties.class, DbosQueueProperties.class})
public class DbosTaskConfiguration {

    @Bean
    DBOSConfig dbosConfig(DbosProperties properties,
                          @Value("${spring.application.name}") String appName) {
        DBOSConfig config = DBOSConfig.defaults(appName)
                .withDatabaseUrl(properties.getJdbcUrl())
                .withDbUser(properties.getUsername())
                .withDbPassword(properties.getPassword())
                .withAdminServer(properties.isAdminServerEnabled())
                .withAdminServerPort(properties.getAdminServerPort());
        DBOS.configure(config);
        return config;
    }

    @Bean
    @DependsOn({"taskDispatchWorkflows", "dbosQueue"})
    DbosLifecycle dbosLifecycle() {
        return new DbosLifecycle();
    }

    @Bean
    DbosStepRunner dbosStepRunner() {
        return new DbosStepRunner();
    }

    // TODO (itaseski): Consider supporting per task type queues for independent concurrency limits
    @Bean
    @DependsOn("dbosConfig")
    Queue dbosQueue(DbosQueueProperties properties) {
        Queue queue = new Queue(properties.getName())
                .withConcurrency(properties.getConcurrency())
                .withPartitionedEnabled(true)
                .withWorkerConcurrency(properties.getWorkerConcurrency());
        DBOS.registerQueue(queue);
        return queue;
    }

    @Bean
    @DependsOn("dbosConfig")
    Map<String, TaskDispatchWorkflow> taskDispatchWorkflows(
            TaskHandlerRegistry registry,
            DbosStepRunner stepRunner,
            ObjectMapper objectMapper,
            CompositeTaskLifecycleListener listener) {
        Map<String, TaskDispatchWorkflow> proxies = new HashMap<>();
        for (TaskHandler<?, ?> handler : registry.all()) {
            TaskDispatchWorkflowImpl impl = new TaskDispatchWorkflowImpl(registry, stepRunner, objectMapper, listener);
            TaskDispatchWorkflow proxy = DBOS.registerWorkflows(TaskDispatchWorkflow.class, impl, handler.name());
            proxies.put(handler.name(), proxy);
        }
        return proxies;
    }

    @Bean
    TaskQueue dbosTaskQueue(
            Map<String, TaskDispatchWorkflow> taskDispatchWorkflows,
            TaskHandlerRegistry registry,
            Queue dbosQueue,
            ObjectMapper objectMapper,
            DbosMapper dbosMapper,
            CompositeTaskLifecycleListener listener) {
        return new DbosTaskQueue(taskDispatchWorkflows, registry, dbosQueue, objectMapper, listener, dbosMapper);
    }
}
