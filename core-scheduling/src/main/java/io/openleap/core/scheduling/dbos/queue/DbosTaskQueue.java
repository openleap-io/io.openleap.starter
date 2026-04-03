package io.openleap.core.scheduling.dbos.queue;

import dev.dbos.transact.DBOS;
import dev.dbos.transact.StartWorkflowOptions;
import dev.dbos.transact.exceptions.DBOSConflictingWorkflowException;
import dev.dbos.transact.exceptions.DBOSNonExistentWorkflowException;
import dev.dbos.transact.execution.ThrowingRunnable;
import dev.dbos.transact.workflow.Queue;
import dev.dbos.transact.workflow.WorkflowHandle;
import dev.dbos.transact.workflow.WorkflowStatus;

import io.openleap.core.scheduling.api.exception.*;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.api.queue.*;
import io.openleap.core.scheduling.dbos.workflow.TaskDispatchWorkflow;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DbosTaskQueue implements TaskQueue {

    private final Map<String, TaskDispatchWorkflow> workflowProxies;
    private final TaskHandlerRegistry registry;
    private final Queue queue;
    private final ObjectMapper objectMapper;
    private final CompositeTaskLifecycleListener listener;
    private final DbosMapper dbosMapper;

    public DbosTaskQueue(Map<String, TaskDispatchWorkflow> workflowProxies,
                         TaskHandlerRegistry registry,
                         Queue queue,
                         ObjectMapper objectMapper,
                         CompositeTaskLifecycleListener listener,
                         DbosMapper dbosMapper) {
        this.workflowProxies = workflowProxies;
        this.registry = registry;
        this.queue = queue;
        this.objectMapper = objectMapper;
        this.listener = listener;
        this.dbosMapper = dbosMapper;
    }

    @Override
    public TaskHandle submit(TaskSubmission submission) {
        if (registry.isAbsent(submission.getHandlerName())) {
            throw new TaskHandlerNotFoundException(submission.getHandlerName());
        }

        String payloadJson;

        try {
            payloadJson = objectMapper.writeValueAsString(submission.getPayload());
        } catch (JacksonException e) {
            throw new TaskSerializationException(submission.getHandlerName(), e);
        }

        String taskId = submission.getTenantId() + "_" + UUID.randomUUID();

        TaskDispatchWorkflow proxy = workflowProxies.get(submission.getHandlerName());

        ThrowingRunnable<Exception> workflowTask = () ->
                proxy.execute(taskId, submission.getTenantId(),
                        submission.getHandlerName(), payloadJson);

        StartWorkflowOptions options = new StartWorkflowOptions()
                .withWorkflowId(taskId)
                .withQueue(queue)
                .withQueuePartitionKey(submission.getTenantId().toString());

        if (submission.getDeduplicationKey() != null) {
            options = options.withDeduplicationId(submission.getDeduplicationKey());
        }

        if (submission.getPriority() != null) {
            options = options.withPriority(submission.getPriority());
        }

        if (submission.getTimeout() != null) {
            options = options.withTimeout(submission.getTimeout());
        }

        try {
            DBOS.startWorkflow(workflowTask, options);
            listener.onSubmitted(taskId, submission.getHandlerName());
            return new TaskHandle(taskId, submission.getHandlerName(), Instant.now());
        } catch (DBOSConflictingWorkflowException _) {
            throw new TaskAlreadyExistsException(taskId, submission.getDeduplicationKey());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R submitAndWait(TaskSubmission submission) {
        TaskHandle handle = submit(submission);
        WorkflowHandle<String, Exception> wfHandle = DBOS.retrieveWorkflow(handle.taskId());
        try {
            // TODO (itaseski): Add better handling if status is not SUCCESS
            String json = wfHandle.getResult();
            if (json == null) {
                return null;
            }
            TaskHandler<?, R> handler = (TaskHandler<?, R>) registry.get(submission.getHandlerName());
            return objectMapper.readValue(json, handler.resultType());
        } catch (TaskExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new TaskExecutionException(handle.taskId(), submission.getHandlerName(), e);
        }
    }

    @Override
    public TaskResult getStatus(String taskId) {
        try {
            WorkflowStatus status = DBOS.getWorkflowStatus(taskId);
            return dbosMapper.toTaskResult(taskId, status);
        } catch (DBOSNonExistentWorkflowException _) {
            throw new TaskNotFoundException(taskId);
        }
    }

    @Override
    public void cancel(String taskId) {
        TaskResult result = getStatus(taskId);
        if (result.status() == TaskStatus.COMPLETED || result.status() == TaskStatus.FAILED) {
            throw new TaskNotCancellableException(taskId, result.status());
        }
        DBOS.cancelWorkflow(taskId);
        listener.onCancelled(taskId);
    }
}
