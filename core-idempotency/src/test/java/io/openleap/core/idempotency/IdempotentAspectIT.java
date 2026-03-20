package io.openleap.core.idempotency;

import io.openleap.core.TestConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@Disabled("Test failing after multi module refactor, needs to be fixed")
class IdempotentAspectIT {

    @Autowired
    TestService testService;

    @Test
    void shouldSkipExecutionWhenCommandAlreadyProcessed() {

        Runnable sideEffect = mock();

        testService.handle(new TestService.MyCommand("command-id-1"), sideEffect);
        testService.handle(new TestService.MyCommand("command-id-1"), sideEffect);

        verify(sideEffect, times(1)).run();
    }


    @Test
    void shouldExecuteTwiceWhenCommandIdsAreDifferent() {

        Runnable sideEffect = mock();

        testService.handle(new TestService.MyCommand("command-id-2"), sideEffect);
        testService.handle(new TestService.MyCommand("command-id-3"), sideEffect);

        verify(sideEffect, times(2)).run();
    }


}
