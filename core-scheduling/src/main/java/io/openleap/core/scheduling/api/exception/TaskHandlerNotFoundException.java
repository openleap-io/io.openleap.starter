package io.openleap.core.scheduling.api.exception;

public class TaskHandlerNotFoundException extends RuntimeException {

    private final String handlerName;

    public TaskHandlerNotFoundException(String handlerName) {
        super("No task handler registered for: " + handlerName);
        this.handlerName = handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }
}
