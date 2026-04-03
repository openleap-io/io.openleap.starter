package io.openleap.core.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Aspect interface for entities with audit trail fields.
 */
public interface Auditable {

    Instant getCreatedAt();

    UUID getCreatedBy();

    Instant getUpdatedAt();

    UUID getUpdatedBy();
}
