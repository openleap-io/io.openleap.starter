package io.openleap.common.messaging.entity;

import io.openleap.common.domain.BusinessId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record OutboxEventId(
        @Column(name = "business_id", nullable = false, updatable = false)
        UUID value
) implements BusinessId {

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

    // TODO (itaseski): Consider removing the toString()
    @Override
    public String toString() {
        return value.toString();
    }
}
