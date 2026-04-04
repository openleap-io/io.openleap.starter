package io.openleap.core.scheduling.api.exception;

public class TaskAlreadyExistsException extends TaskException {

    private final String deduplicationKey;

    public TaskAlreadyExistsException(String taskId, String deduplicationKey) {
        super(taskId, "Task already exists for deduplication key: " + deduplicationKey);
        this.deduplicationKey = deduplicationKey;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }
}
