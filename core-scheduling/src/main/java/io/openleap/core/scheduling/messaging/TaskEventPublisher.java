package io.openleap.core.scheduling.messaging;

import io.openleap.core.messaging.RoutingKey;
import io.openleap.core.messaging.event.BaseDomainEvent;
import io.openleap.core.messaging.event.EventPublisher;
import io.openleap.core.scheduling.api.listener.TaskLifecycleListener;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Map;

// TODO (itaseski): Write IT with RabbitMQ running
public class TaskEventPublisher implements TaskLifecycleListener {

    private final RoutingKey taskSubmitted;
    private final RoutingKey taskCompleted;
    private final RoutingKey taskFailed;
    private final RoutingKey taskCancelled;
    private final EventPublisher eventPublisher;

    public TaskEventPublisher(EventPublisher eventPublisher,
                              @Value("${spring.application.name}") String appName) {
        this.eventPublisher = eventPublisher;
        this.taskSubmitted = RoutingKey.of(appName + TaskEvents.ROUTING_SUBMITTED);
        this.taskCompleted = RoutingKey.of(appName + TaskEvents.ROUTING_COMPLETED);
        this.taskFailed = RoutingKey.of(appName + TaskEvents.ROUTING_FAILED);
        this.taskCancelled = RoutingKey.of(appName + TaskEvents.ROUTING_CANCELLED);
    }

    @Override
    public void onSubmitted(String taskId, String handlerName) {
        eventPublisher.enqueue(taskSubmitted, event(taskId, TaskEvents.SUBMITTED,
                Map.of(TaskEvents.META_HANDLER, handlerName)), Collections.emptyMap());
    }

    @Override
    public void onCompleted(String taskId, String handlerName) {
        eventPublisher.enqueue(taskCompleted, event(taskId, TaskEvents.COMPLETED,
                Map.of(TaskEvents.META_HANDLER, handlerName)), Collections.emptyMap());
    }

    @Override
    public void onFailed(String taskId, String handlerName, Throwable error) {
        eventPublisher.enqueue(taskFailed, event(taskId, TaskEvents.FAILED,
                Map.of(TaskEvents.META_HANDLER, handlerName, TaskEvents.META_CAUSE, error.getMessage())), Collections.emptyMap());
    }

    @Override
    public void onCancelled(String taskId) {
        eventPublisher.enqueue(taskCancelled, event(taskId, TaskEvents.CANCELLED), Collections.emptyMap());
    }

    private static BaseDomainEvent event(String taskId, String changeType) {
        return event(taskId, changeType, Collections.emptyMap());
    }

    private static BaseDomainEvent event(String taskId, String changeType, Map<String, Object> metadata) {
        return BaseDomainEvent.builder()
                .aggregateId(taskId)
                .aggregateType(TaskEvents.AGGREGATE_TYPE)
                .changeType(changeType)
                .metadata(metadata)
                .build();
    }
}
