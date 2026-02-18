package io.openleap.common.idempotency.exception;

public class DuplicateCommandException extends RuntimeException {

    public DuplicateCommandException(String message) {
        super(message);
    }

    public DuplicateCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
