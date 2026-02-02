package io.openleap.common.messaging.command;

import io.openleap.common.util.OpenleapUuid;

import java.util.UUID;

public record CommandId(UUID value) {
    public static CommandId of(UUID value) {
        return new CommandId(value);
    }

    public static CommandId of(String uuid) {
        return new CommandId(UUID.fromString(uuid));
    }

    public static CommandId create() {
        return new CommandId(OpenleapUuid.create());
    }
    public static CommandId generate() {
        return new CommandId(OpenleapUuid.create());
    }
}
