package io.openleap.core.scheduling.api.handler;

// TODO (itaseski): Add differentiation between internal and external handlers
public interface TaskHandler<P, R> {

    String name();

    Class<P> payloadType();

    Class<R> resultType();

    R handle(P payload, StepRunner steps);

    // TODO (itaseski): Add method for per handler json schema validation
}