package io.openleap.core.scheduling.dbos.workflow;

import dev.dbos.transact.workflow.Workflow;
import io.openleap.core.scheduling.api.exception.TaskExecutionException;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.dbos.step.DbosStepRunner;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

public class TaskDispatchWorkflowImpl implements TaskDispatchWorkflow {

    private final TaskHandlerRegistry registry;
    private final DbosStepRunner stepRunner;
    private final ObjectMapper objectMapper;
    private final CompositeTaskLifecycleListener listener;

    public TaskDispatchWorkflowImpl(TaskHandlerRegistry registry,
                                    DbosStepRunner stepRunner,
                                    ObjectMapper objectMapper,
                                    CompositeTaskLifecycleListener listener) {
        this.registry = registry;
        this.stepRunner = stepRunner;
        this.objectMapper = objectMapper;
        this.listener = listener;
    }

    @Workflow(name = "handler")
    @Override
    public String execute(String taskId, UUID tenantId, String handlerName, String payloadJson) {
        boolean success = false;
        try {
            @SuppressWarnings("unchecked")
            TaskHandler<Object, Object> handler = (TaskHandler<Object, Object>) registry.get(handlerName);
            Object payload = objectMapper.readValue(payloadJson, handler.payloadType());
            // TODO (itaseski): Perform field validation on the handler payload? Provide Json schema per handler?
            Object result = handler.handle(payload, stepRunner);
            String resultJson = result != null ? objectMapper.writeValueAsString(result) : null;
            success = true;
            return resultJson;
        } catch (TaskExecutionException e) {
            listener.onFailed(taskId, handlerName, e);
            throw e;
        } catch (Exception e) {
            TaskExecutionException wrapped = new TaskExecutionException(taskId, handlerName, e);
            listener.onFailed(taskId, handlerName, wrapped);
            throw wrapped;
        } finally {
            if (success) {
                listener.onCompleted(taskId, handlerName);
            }
        }
    }
}
