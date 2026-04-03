package io.openleap.core.extension.spi;

import java.util.List;
import java.util.Map;

/**
 * SPI for addon JARs to define custom field schemas and validation logic
 * for platform aggregates.
 *
 * <p>Implementations must be Spring {@code @Component} beans so that they
 * are auto-discovered by the {@link io.openleap.core.extension.registry.CustomFieldRegistry}.
 *
 * @param <T> the aggregate entity type this provider extends
 */
public interface CustomFieldProvider<T> {

    /**
     * The aggregate entity type this provider extends.
     */
    Class<T> aggregateType();

    /**
     * Field definitions (schema) for this aggregate type.
     * Used by the BFF for UI rendering and field-level security.
     */
    List<FieldDefinition> getFields();

    /**
     * Validate custom field values. Returns {@link ValidationResult#ok()} when valid.
     * <p>Must complete within 50ms.
     */
    ValidationResult validate(Map<String, Object> customFields);
}
