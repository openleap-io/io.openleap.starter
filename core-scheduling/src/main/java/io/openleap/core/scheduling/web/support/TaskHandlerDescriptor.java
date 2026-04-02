package io.openleap.core.scheduling.web.support;

import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.web.dto.HandlerInfoResponse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class TaskHandlerDescriptor {

    private TaskHandlerDescriptor() {
    }

    // TODO (itaseski): Only goes one level deep. Nested objects are not recursed into.
    public static HandlerInfoResponse describe(TaskHandler<?, ?> handler) {
        Class<?> payloadType = handler.payloadType();
        Class<?> resultType = handler.resultType();
        return new HandlerInfoResponse(
                handler.name(),
                payloadType.getSimpleName(),
                resolveFields(payloadType),
                resultType.getSimpleName(),
                resultType == Void.class ? Map.of() : resolveFields(resultType)
        );
    }

    private static Map<String, String> resolveFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, f -> f.getType().getSimpleName()));
    }
}
