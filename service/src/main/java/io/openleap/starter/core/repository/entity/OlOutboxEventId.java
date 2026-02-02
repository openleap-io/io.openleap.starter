package io.openleap.starter.core.repository.entity;

import io.openleap.starter.core.domain.OlBusinessId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record OlOutboxEventId(
        @Column(name = "business_id", nullable = false, updatable = false)
        UUID value
) implements OlBusinessId {

    public OlOutboxEventId {
        if (value == null) {
            throw new IllegalArgumentException("OutboxEventId value cannot be null");
        }
    }

    public static OlOutboxEventId create() {
        return new OlOutboxEventId(UUID.randomUUID());
    }

    public static OlOutboxEventId of(UUID value) {
        return new OlOutboxEventId(value);
    }

    public static OlOutboxEventId parse(String value) {
        return new OlOutboxEventId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
