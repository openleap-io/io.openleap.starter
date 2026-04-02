package io.openleap.core.scheduling.api.exception;

public class TaskQueueFullException extends RuntimeException {

    private final String queueName;

    public TaskQueueFullException(String queueName) {
        super("Task queue is full: " + queueName);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }
}
