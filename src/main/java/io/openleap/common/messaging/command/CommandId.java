package io.openleap.common.messaging.command;

import io.openleap.common.util.UuidUtils;

import java.util.UUID;

public record CommandId(UUID value) {
    public static CommandId of(UUID value) {
        return new CommandId(value);
    }

    public static CommandId of(String uuid) {
        return new CommandId(UUID.fromString(uuid));
    }

    public static CommandId create() {
        return new CommandId(UuidUtils.create());
    }
    public static CommandId generate() {
        return new CommandId(UuidUtils.create());
    }
}
