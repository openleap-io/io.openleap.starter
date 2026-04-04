package io.openleap.core.scheduling.api.exception;

public class TaskException extends RuntimeException {

    private final String taskId;

    public TaskException(String taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    public TaskException(String taskId, String message, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
