package io.openleap.core.scheduling.dbos.step;

import dev.dbos.transact.DBOS;
import dev.dbos.transact.execution.ThrowingSupplier;
import dev.dbos.transact.workflow.StepOptions;
import io.openleap.core.scheduling.api.handler.RetryOptions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class DbosStepRunnerTest {

    private final DbosStepRunner runner = new DbosStepRunner();

    @Test
    void run_callable_executesStepAndReturnsResult() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.runStep(any(ThrowingSupplier.class), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, ThrowingSupplier.class).execute());

            String result = runner.run("my-step", () -> "hello");

            assertEquals("hello", result);
            dbos.verify(() -> DBOS.runStep(any(ThrowingSupplier.class), eq("my-step")));
        }
    }

    @Test
    void run_runnable_executesStep() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.runStep(any(ThrowingSupplier.class), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, ThrowingSupplier.class).execute());

            Runnable step = mock(Runnable.class);
            runner.run("my-step", step);

            verify(step, times(1)).run();
            dbos.verify(() -> DBOS.runStep(any(ThrowingSupplier.class), eq("my-step")));
        }
    }

    @Test
    void run_callable_withRetryOptions_mapsStepOptionsCorrectly() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.runStep(any(ThrowingSupplier.class), any(StepOptions.class)))
                    .thenAnswer(inv -> inv.getArgument(0, ThrowingSupplier.class).execute());

            RetryOptions retryOptions = new RetryOptions(3, 1.0, 2.0);
            runner.run("my-step", () -> "result", retryOptions);

            ArgumentCaptor<StepOptions> captor = ArgumentCaptor.forClass(StepOptions.class);
            dbos.verify(() -> DBOS.runStep(any(ThrowingSupplier.class), captor.capture()));

            StepOptions options = captor.getValue();
            assertTrue(options.retriesAllowed());
            assertEquals(3, options.maxAttempts());
            assertEquals(1.0, options.intervalSeconds());
            assertEquals(2.0, options.backOffRate());
        }
    }

    @Test
    void run_runnable_withRetryOptions_mapsStepOptionsCorrectly() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.runStep(any(ThrowingSupplier.class), any(StepOptions.class)))
                    .thenAnswer(inv -> inv.getArgument(0, ThrowingSupplier.class).execute());

            RetryOptions retryOptions = new RetryOptions(3, 1.0, 2.0);
            runner.run("my-step", () -> {}, retryOptions);

            ArgumentCaptor<StepOptions> captor = ArgumentCaptor.forClass(StepOptions.class);
            dbos.verify(() -> DBOS.runStep(any(ThrowingSupplier.class), captor.capture()));

            StepOptions options = captor.getValue();
            assertTrue(options.retriesAllowed());
            assertEquals(3, options.maxAttempts());
            assertEquals(1.0, options.intervalSeconds());
            assertEquals(2.0, options.backOffRate());
        }
    }
}
