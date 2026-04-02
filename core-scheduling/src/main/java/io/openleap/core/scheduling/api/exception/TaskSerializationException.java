package io.openleap.core.scheduling.api.exception;

public class TaskSerializationException extends RuntimeException {

    private final String handlerName;

    public TaskSerializationException(String handlerName, Throwable cause) {
        super("Failed to serialize payload for handler: " + handlerName, cause);
        this.handlerName = handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }
}
