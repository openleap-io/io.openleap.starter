package io.openleap.core.scheduling.support;

import io.openleap.core.scheduling.api.handler.StepRunner;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestHandler implements TaskHandler<TestHandler.Payload, TestHandler.Result> {

    public record Payload(String message) {}

    public record Result(String echo) {}

    @Override
    public String name() { return "test-handler"; }

    @Override
    public Class<Payload> payloadType() { return Payload.class; }

    @Override
    public Class<Result> resultType() { return Result.class; }

    @Override
    public Result handle(Payload payload, StepRunner steps) {
        return new Result(payload.message());
    }

}