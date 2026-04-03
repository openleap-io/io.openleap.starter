package io.openleap.core.scheduling.api.queue;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class TaskSubmission {

    private final UUID tenantId;
    private final String handlerName;
    private final Object payload;
    private final String deduplicationKey;
    private final Integer priority;
    private final Duration timeout;

    private TaskSubmission(Builder builder) {
        this.tenantId = Objects.requireNonNull(builder.tenantId, "tenantId is required");
        this.handlerName = Objects.requireNonNull(builder.handlerName, "handlerName is required");
        this.payload = Objects.requireNonNull(builder.payload, "payload is required");
        this.deduplicationKey = builder.deduplicationKey;
        this.priority = builder.priority;
        this.timeout = builder.timeout;
    }

    public static Builder forHandler(String handlerName) {
        return new Builder(handlerName);
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public Object getPayload() {
        return payload;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public Integer getPriority() {
        return priority;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public static final class Builder {
        private final String handlerName;
        private UUID tenantId;
        private Object payload;
        private String deduplicationKey;
        private Integer priority;
        private Duration timeout;

        private Builder(String handlerName) {
            this.handlerName = handlerName;
        }

        public Builder tenant(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder deduplicationKey(String key) {
            this.deduplicationKey = key;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public TaskSubmission build() {
            return new TaskSubmission(this);
        }
    }
}

