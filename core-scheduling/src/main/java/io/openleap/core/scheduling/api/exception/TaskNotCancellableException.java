package io.openleap.core.scheduling.api.exception;

import io.openleap.core.scheduling.api.queue.TaskStatus;

public class TaskNotCancellableException extends TaskException {

    private final TaskStatus currentStatus;

    public TaskNotCancellableException(String taskId, TaskStatus currentStatus) {
        super(taskId, "Task " + taskId + " cannot be cancelled, current status: " + currentStatus);
        this.currentStatus = currentStatus;
    }

    public TaskStatus getCurrentStatus() {
        return currentStatus;
    }
}
