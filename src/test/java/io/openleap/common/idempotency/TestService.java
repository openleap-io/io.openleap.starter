package io.openleap.common.idempotency;

import io.openleap.common.idempotency.aspect.Idempotent;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestService {

    @Idempotent(keyExpression = "#command.commandId")
    public void handle(MyCommand command, Runnable sideEffect) {
        sideEffect.run();
    }

    public record MyCommand(String commandId) {}

}
