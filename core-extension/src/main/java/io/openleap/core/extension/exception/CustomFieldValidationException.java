package io.openleap.core.extension.exception;

import io.openleap.core.extension.spi.ValidationResult;

/**
 * Thrown when custom field validation fails.
 * Maps to HTTP 422 Unprocessable Entity in the global exception handler.
 */
public class CustomFieldValidationException extends RuntimeException {

    private final ValidationResult validationResult;

    public CustomFieldValidationException(ValidationResult validationResult) {
        super("Custom field validation failed");
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
