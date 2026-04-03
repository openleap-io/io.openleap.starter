package io.openleap.core.extension.registry;

import io.openleap.core.extension.spi.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CustomFieldRegistryTest {

    // Dummy entity types for testing
    static class OrderEntity {}
    static class ProductEntity {}

    @Test
    void emptyRegistry_returnsEmptyFieldsAndOkValidation() {
        var registry = new CustomFieldRegistry(List.of());

        assertThat(registry.getFieldsFor(OrderEntity.class)).isEmpty();
        assertThat(registry.validateFor(OrderEntity.class, Map.of()).isValid()).isTrue();
    }

    @Test
    void singleProvider_returnsFieldDefinitions() {
        var provider = new TestProvider(OrderEntity.class,
                List.of(new FieldDefinition("color", FieldType.STRING, "Color", false,
                        null, null, null, null, null)));

        var registry = new CustomFieldRegistry(List.of(provider));

        var fields = registry.getFieldsFor(OrderEntity.class);
        assertThat(fields).hasSize(1);
        assertThat(fields.getFirst().key()).isEqualTo("color");
    }

    @Test
    void singleProvider_validatesCustomFields() {
        var provider = new TestProvider(OrderEntity.class, List.of()) {
            @Override
            public ValidationResult validate(Map<String, Object> customFields) {
                if (customFields.containsKey("bad")) {
                    return ValidationResult.fail("bad", "not allowed");
                }
                return ValidationResult.ok();
            }
        };

        var registry = new CustomFieldRegistry(List.of(provider));

        assertThat(registry.validateFor(OrderEntity.class, Map.of()).isValid()).isTrue();
        assertThat(registry.validateFor(OrderEntity.class, Map.of("bad", "value")).isValid()).isFalse();
    }

    @Test
    void unknownType_returnsEmptyAndOk() {
        var provider = new TestProvider(OrderEntity.class, List.of());
        var registry = new CustomFieldRegistry(List.of(provider));

        assertThat(registry.getFieldsFor(ProductEntity.class)).isEmpty();
        assertThat(registry.validateFor(ProductEntity.class, Map.of("x", "y")).isValid()).isTrue();
    }

    @Test
    void nullCustomFields_treatedAsEmpty() {
        var provider = new TestProvider(OrderEntity.class, List.of());
        var registry = new CustomFieldRegistry(List.of(provider));

        assertThat(registry.validateFor(OrderEntity.class, null).isValid()).isTrue();
    }

    @Test
    void duplicateKeys_failsAtStartup() {
        var provider1 = new TestProvider(OrderEntity.class,
                List.of(new FieldDefinition("dup", FieldType.STRING, "Dup", false,
                        null, null, null, null, null)));
        var provider2 = new TestProvider(OrderEntity.class,
                List.of(new FieldDefinition("dup", FieldType.INTEGER, "Dup", false,
                        null, null, null, null, null)));

        assertThatThrownBy(() -> new CustomFieldRegistry(List.of(provider1, provider2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate custom field key 'dup'");
    }

    @Test
    void multipleProviders_aggregateFieldsAndValidation() {
        var provider1 = new TestProvider(OrderEntity.class,
                List.of(new FieldDefinition("field1", FieldType.STRING, "F1", false,
                        null, null, null, null, null)));
        var provider2 = new TestProvider(OrderEntity.class,
                List.of(new FieldDefinition("field2", FieldType.INTEGER, "F2", false,
                        null, null, null, null, null)));

        var registry = new CustomFieldRegistry(List.of(provider1, provider2));

        assertThat(registry.getFieldsFor(OrderEntity.class)).hasSize(2);
    }

    // Helper test implementation
    @SuppressWarnings("unchecked")
    private static class TestProvider implements CustomFieldProvider<Object> {
        private final Class<?> type;
        private final List<FieldDefinition> fields;

        TestProvider(Class<?> type, List<FieldDefinition> fields) {
            this.type = type;
            this.fields = fields;
        }

        @Override
        public Class<Object> aggregateType() {
            return (Class<Object>) type;
        }

        @Override
        public List<FieldDefinition> getFields() {
            return fields;
        }

        @Override
        public ValidationResult validate(Map<String, Object> customFields) {
            return ValidationResult.ok();
        }
    }
}
