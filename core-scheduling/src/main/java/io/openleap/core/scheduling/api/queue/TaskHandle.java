package io.openleap.core.scheduling.api.queue;

import java.time.Instant;

public record TaskHandle(
        String taskId,
        String handlerName,
        Instant submittedAt
) {}
