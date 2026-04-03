package io.openleap.core.persistence.entity;

import java.util.Map;

/**
 * Aspect interface for entities that support product-specific custom fields
 * stored as JSONB.
 */
public interface Extensible {

    Map<String, Object> getCustomFields();

    void setCustomFields(Map<String, Object> customFields);
}
