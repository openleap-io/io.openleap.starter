package io.openleap.core.scheduling.api.queue;

import java.time.Instant;

public record TaskResult(
        String taskId,
        TaskStatus status,
        Instant submittedAt,
        Instant startedAt,
        Instant completedAt,
        String errorCode,
        String errorMessage
) {

    // TODO (itaseski): Double check if all values make sense per STATUS
    public static TaskResult unknown(String taskId) {
        return new TaskResult(taskId, TaskStatus.UNKNOWN, null, null, null, null, null);
    }

    public static TaskResult pending(String taskId, Instant submittedAt) {
        return new TaskResult(taskId, TaskStatus.PENDING, submittedAt, null, null, null, null);
    }

    public static TaskResult running(String taskId, Instant submittedAt, Instant startedAt) {
        return new TaskResult(taskId, TaskStatus.RUNNING, submittedAt, startedAt, null, null, null);
    }

    public static TaskResult completed(String taskId, Instant submittedAt, Instant startedAt) {
        return new TaskResult(taskId, TaskStatus.COMPLETED, submittedAt, startedAt, Instant.now(), null, null);
    }

    public static TaskResult failed(String taskId, Instant submittedAt, Instant startedAt, Throwable error) {
        return new TaskResult(taskId, TaskStatus.FAILED, submittedAt, startedAt, Instant.now(),
                error.getClass().getSimpleName(), error.getMessage());
    }

    public static TaskResult cancelled(String taskId, Instant submittedAt, Instant startedAt) {
        return new TaskResult(taskId, TaskStatus.CANCELLED, submittedAt, startedAt, Instant.now(), null, null);
    }
}
