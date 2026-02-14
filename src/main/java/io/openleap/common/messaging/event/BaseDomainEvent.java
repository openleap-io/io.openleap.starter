package io.openleap.common.messaging.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Generic, minimal message payload used in informational events.
 * Concrete domain payloads should extend this type to keep a consistent envelope.
 */
@Getter
@EqualsAndHashCode
@ToString
public class BaseDomainEvent implements DomainEvent {

    private final String aggregateId;
    private final String aggregateType;
    private final String changeType;
    private final Long version;
    private final Instant occurredAt;
    private final Map<String, Object> metadata;

    protected BaseDomainEvent() {
        this.aggregateId = null;
        this.aggregateType = null;
        this.changeType = null;
        this.version = null;
        this.occurredAt = null;
        this.metadata = null;
    }

    protected BaseDomainEvent(String aggregateId,
                              String aggregateType,
                              String changeType,
                              Long version,
                              Instant occurredAt,
                              Map<String, Object> metadata) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.changeType = changeType;
        this.version = version;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String aggregateId;
        private String aggregateType;
        private String changeType;
        private Long version;
        private Instant occurredAt;
        private Map<String, Object> metadata;

        private Builder() {
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder changeType(String changeType) {
            this.changeType = changeType;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public BaseDomainEvent build() {
            return new BaseDomainEvent(aggregateId, aggregateType, changeType, version, occurredAt, metadata);
        }
    }
}
