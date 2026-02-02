package io.openleap.starter.core.lock.exception;

public enum LockError {

    PROCESS_WITH_LOCK_IS_ALREADY_RUNNING("Cannot start process: the lock is already held by another instance.");

    private final String description;

    LockError(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
