package io.openleap.core.scheduling.api.exception;

public class TaskNotFoundException extends TaskException {

    public TaskNotFoundException(String taskId) {
        super(taskId, "Task not found: " + taskId);
    }
}
