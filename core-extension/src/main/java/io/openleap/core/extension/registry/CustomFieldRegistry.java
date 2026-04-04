package io.openleap.core.extension.registry;

import io.openleap.core.extension.spi.CustomFieldProvider;
import io.openleap.core.extension.spi.FieldDefinition;
import io.openleap.core.extension.spi.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Auto-discovers all {@link CustomFieldProvider} beans and dispatches
 * field definition and validation requests by aggregate type.
 *
 * <p>When no providers are registered, all calls return empty/ok (zero overhead).
 */
public class CustomFieldRegistry {

    private static final Logger log = LoggerFactory.getLogger(CustomFieldRegistry.class);

    private final Map<Class<?>, List<CustomFieldProvider<?>>> providersByType;

    public CustomFieldRegistry(List<CustomFieldProvider<?>> providers) {
        this.providersByType = providers.stream()
                .collect(Collectors.groupingBy(CustomFieldProvider::aggregateType));

        detectDuplicateKeys();

        log.info("CustomFieldRegistry initialized with {} provider(s) for {} aggregate type(s)",
                providers.size(), providersByType.size());
    }

    /**
     * Get field definitions for an aggregate type.
     * Returns an empty list if no providers are registered for this type.
     */
    public List<FieldDefinition> getFieldsFor(Class<?> aggregateType) {
        var providers = providersByType.getOrDefault(aggregateType, List.of());
        return providers.stream()
                .flatMap(p -> p.getFields().stream())
                .toList();
    }

    /**
     * Validate custom fields for an aggregate type.
     * Returns {@link ValidationResult#ok()} if no providers are registered.
     */
    public ValidationResult validateFor(Class<?> aggregateType, Map<String, Object> customFields) {
        var providers = providersByType.getOrDefault(aggregateType, List.of());
        if (providers.isEmpty()) {
            return ValidationResult.ok();
        }

        var allErrors = new ArrayList<ValidationResult.FieldError>();
        for (var provider : providers) {
            var result = provider.validate(customFields != null ? customFields : Map.of());
            if (!result.isValid()) {
                allErrors.addAll(result.fieldErrors());
            }
        }

        return allErrors.isEmpty()
                ? ValidationResult.ok()
                : ValidationResult.fail(allErrors);
    }

    private void detectDuplicateKeys() {
        for (var entry : providersByType.entrySet()) {
            var seenKeys = new HashSet<String>();
            for (var provider : entry.getValue()) {
                for (var field : provider.getFields()) {
                    if (!seenKeys.add(field.key())) {
                        throw new IllegalStateException(
                                "Duplicate custom field key '%s' for aggregate type %s"
                                        .formatted(field.key(), entry.getKey().getSimpleName()));
                    }
                }
            }
        }
    }
}
