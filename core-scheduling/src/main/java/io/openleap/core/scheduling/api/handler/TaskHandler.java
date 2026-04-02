package io.openleap.core.scheduling.api.handler;

public interface TaskHandler<P, R> {

    String name();

    Class<P> payloadType();

    Class<R> resultType();

    R handle(P payload, StepRunner steps);
}