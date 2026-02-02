package io.openleap.starter.core.messaging.command;

import io.openleap.starter.core.util.OlUuid;

import java.util.UUID;

public record OlCommandId(UUID value) {
    public static OlCommandId of(UUID value) {
        return new OlCommandId(value);
    }

    public static OlCommandId of(String uuid) {
        return new OlCommandId(UUID.fromString(uuid));
    }

    public static OlCommandId create() {
        return new OlCommandId(OlUuid.create());
    }
    public static OlCommandId generate() {
        return new OlCommandId(OlUuid.create());
    }
}
