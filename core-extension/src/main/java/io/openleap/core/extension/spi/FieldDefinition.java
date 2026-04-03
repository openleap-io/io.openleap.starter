package io.openleap.core.extension.spi;

import java.util.List;

public record FieldDefinition(
        String key,
        FieldType type,
        String label,
        boolean required,
        List<String> validValues,
        Number min,
        Number max,
        String readPermission,
        String writePermission
) {

    public FieldDefinition {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Field key must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Field type must not be null");
        }
    }
}
