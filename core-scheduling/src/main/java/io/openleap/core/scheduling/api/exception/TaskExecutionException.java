package io.openleap.core.scheduling.api.exception;

public class TaskExecutionException extends TaskException {

    private final String handlerName;

    public TaskExecutionException(String taskId, String handlerName, Throwable cause) {
        super(taskId, "Task execution failed for handler '" + handlerName + "'", cause);
        this.handlerName = handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }
}
