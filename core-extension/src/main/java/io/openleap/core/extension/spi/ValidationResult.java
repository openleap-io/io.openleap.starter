package io.openleap.core.extension.spi;

import java.util.List;

public record ValidationResult(boolean valid, List<FieldError> fieldErrors) {

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(List<FieldError> fieldErrors) {
        return new ValidationResult(false, fieldErrors);
    }

    public static ValidationResult fail(String field, String message) {
        return new ValidationResult(false, List.of(new FieldError(field, message)));
    }

    public boolean isValid() {
        return valid;
    }

    public record FieldError(String field, String message) {}
}
