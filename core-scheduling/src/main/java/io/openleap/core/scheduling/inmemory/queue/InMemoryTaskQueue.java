package io.openleap.core.scheduling.inmemory.queue;

import io.openleap.core.scheduling.api.exception.*;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.api.queue.*;
import io.openleap.core.scheduling.inmemory.step.DirectStepRunner;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

// TODO (itaseski): Add support for periodical cleanup of tasks and futures
public class InMemoryTaskQueue implements TaskQueue {

    private final TaskHandlerRegistry registry;
    private final ExecutorService executor;
    private final DirectStepRunner stepRunner;
    private final CompositeTaskLifecycleListener listener;
    private final JsonMapper jsonMapper;
    private final Map<String, TaskResult> tasks = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, String> deduplicationKeys = new ConcurrentHashMap<>();

    public InMemoryTaskQueue(TaskHandlerRegistry registry,
                             ExecutorService executor,
                             DirectStepRunner stepRunner,
                             CompositeTaskLifecycleListener listener,
                             JsonMapper jsonMapper) {
        this.registry = registry;
        this.executor = executor;
        this.stepRunner = stepRunner;
        this.listener = listener;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public TaskHandle submit(TaskSubmission submission) {
        if (registry.isAbsent(submission.getHandlerName())) {
            throw new TaskHandlerNotFoundException(submission.getHandlerName());
        }

        if (submission.getDeduplicationKey() != null) {
            String existingTaskId = deduplicationKeys.get(submission.getDeduplicationKey());
            if (existingTaskId != null) {
                throw new TaskAlreadyExistsException(existingTaskId, submission.getDeduplicationKey());
            }
        }

        String taskId = submission.getTenantId() + "_" + UUID.randomUUID();

        if (submission.getDeduplicationKey() != null) {
            deduplicationKeys.put(submission.getDeduplicationKey(), taskId);
        }

        Instant submittedAt = Instant.now();

        tasks.put(taskId, TaskResult.pending(taskId, submittedAt));

        listener.onSubmitted(taskId, submission.getHandlerName());

        Future<Object> future = executor.submit(() -> {
            Instant startedAt = Instant.now();
            tasks.put(taskId, TaskResult.running(taskId, submittedAt, startedAt));
            try {
                @SuppressWarnings("unchecked")
                TaskHandler<Object, Object> handler = (TaskHandler<Object, Object>) registry.get(submission.getHandlerName());
                Object payload = jsonMapper.convertValue(submission.getPayload(), handler.payloadType());
                Object result = handler.handle(payload, stepRunner);
                tasks.put(taskId, TaskResult.completed(taskId, submittedAt, startedAt));
                listener.onCompleted(taskId, submission.getHandlerName());
                return result;
            } catch (Exception e) {
                tasks.put(taskId, TaskResult.failed(taskId, submittedAt, startedAt, e));
                listener.onFailed(taskId, submission.getHandlerName(), e);
                throw new TaskExecutionException(taskId, submission.getHandlerName(), e);
            } finally {
                // TODO (itaseski): There is a race condition here if remove() executes before put()
                //  think of a way to avoid this and make sure that put() is not called after remove()
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);

        return new TaskHandle(taskId, submission.getHandlerName(), Instant.now());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R submitAndWait(TaskSubmission submission) {
        TaskHandle handle = submit(submission);
        // TODO (itaseski): If future is removed before we come to this block it will return a null
        Future<?> future = futures.get(handle.taskId());
        if (future == null) {
            return null;
        }
        try {
            Object result = future.get();
            TaskHandler<?, R> handler = (TaskHandler<?, R>) registry.get(submission.getHandlerName());
            return handler.resultType() == Void.class ? null : handler.resultType().cast(result);
        } catch (ExecutionException e) {
            throw new TaskExecutionException(handle.taskId(), submission.getHandlerName(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException(handle.taskId(), submission.getHandlerName(), e);
        }
    }

    @Override
    public TaskResult getStatus(String taskId) {
        TaskResult result = tasks.get(taskId);
        if (result == null) {
            throw new TaskNotFoundException(taskId);
        }
        return result;
    }

    @Override
    public void cancel(String taskId) {
        TaskResult result = getStatus(taskId);
        if (result.status() == TaskStatus.COMPLETED || result.status() == TaskStatus.FAILED) {
            throw new TaskNotCancellableException(taskId, result.status());
        }
        Future<?> future = futures.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
        tasks.put(taskId, TaskResult.cancelled(taskId, result.submittedAt(), result.startedAt()));
        listener.onCancelled(taskId);
    }
}
