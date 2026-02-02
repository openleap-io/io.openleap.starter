package io.openleap.common.lock.exception;

public class ConcurrentExecutionException extends RuntimeException {

    private final LockError error;

    public ConcurrentExecutionException(LockError error) {
        super(error.getDescription());
        this.error = error;
    }


    public ConcurrentExecutionException(LockError error, Throwable cause) {
        super(error.getDescription(), cause);
        this.error = error;
    }

    public LockError getError() {
        return error;
    }
}
