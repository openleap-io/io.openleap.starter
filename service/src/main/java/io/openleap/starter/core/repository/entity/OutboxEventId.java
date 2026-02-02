package io.openleap.starter.core.repository.entity;

import io.openleap.starter.core.domain.OlBusinessId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record OutboxEventId(
        @Column(name = "business_id", nullable = false, updatable = false)
        UUID value
) implements OlBusinessId {

    public OutboxEventId {
        if (value == null) {
            throw new IllegalArgumentException("OutboxEventId value cannot be null");
        }
    }

    public static OutboxEventId create() {
        return new OutboxEventId(UUID.randomUUID());
    }

    public static OutboxEventId of(UUID value) {
        return new OutboxEventId(value);
    }

    public static OutboxEventId parse(String value) {
        return new OutboxEventId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
