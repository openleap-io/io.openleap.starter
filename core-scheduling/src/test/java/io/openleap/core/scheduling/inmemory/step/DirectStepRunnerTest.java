package io.openleap.core.scheduling.inmemory.step;

import io.openleap.core.scheduling.api.handler.RetryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DirectStepRunnerTest {

    private DirectStepRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DirectStepRunner();
    }

    @Test
    void run_callable_returnsResult() {
        String result = runner.run("step", () -> "done");

        assertThat(result).isEqualTo("done");
    }

    @Test
    void run_callable_wrapsExceptionInRuntimeException() {
        Callable<String> failing = () -> { throw new Exception("oops"); };

        assertThatThrownBy(() -> runner.run("step", failing))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void run_runnable_executesStep() {
        AtomicBoolean executed = new AtomicBoolean(false);

        runner.run("step", () -> executed.set(true));

        assertThat(executed).isTrue();
    }

    @Test
    void run_callableWithRetry_returnsResult() {
        RetryOptions retryOptions = RetryOptions.of().withMaxAttempts(3).withIntervalSeconds(0.1);

        String result = runner.run("step", () -> "done", retryOptions);

        assertThat(result).isEqualTo("done");
    }

    @Test
    void run_callableWithRetry_retriesOnFailureThenSucceeds() {
        RetryOptions retryOptions = RetryOptions.of().withMaxAttempts(3).withIntervalSeconds(0.1);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        // needed since variables in lambda need to be effectively final
        int[] attempts = {0};

        runner.run("step", () -> {
            if (++attempts[0] < 3) {
                throw new Exception("transient");
            }
            succeeded.set(true);
            return null;
        }, retryOptions);

        assertThat(succeeded).isTrue();
        assertThat(attempts[0]).isEqualTo(3);
    }

    @Test
    void run_callableWithRetry_wrapsExceptionAfterExhaustedAttempts() {
        RetryOptions retryOptions = RetryOptions.of().withMaxAttempts(2).withIntervalSeconds(0.1);
        Callable<Void> failing = () -> { throw new Exception("fail"); };

        assertThatThrownBy(() -> runner.run("step", failing, retryOptions))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void run_runnableWithRetry_executesStep() {
        RetryOptions retryOptions = RetryOptions.of().withMaxAttempts(3).withIntervalSeconds(0.1);
        AtomicBoolean executed = new AtomicBoolean(false);

        runner.run("step", () -> executed.set(true), retryOptions);

        assertThat(executed).isTrue();
    }

    @Test
    void run_runnableWithRetry_retriesOnFailureThenSucceeds() {
        RetryOptions retryOptions = RetryOptions.of().withMaxAttempts(3).withIntervalSeconds(0.1);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        // needed since variables in lambda need to be effectively final
        int[] attempts = {0};

        runner.run("step", () -> {
            if (++attempts[0] < 3) throw new RuntimeException("transient");
            succeeded.set(true);
        }, retryOptions);

        assertThat(succeeded).isTrue();
        assertThat(attempts[0]).isEqualTo(3);
    }

    @Test
    void run_runnableWithRetry_wrapsExceptionAfterExhaustedAttempts() {
        RetryOptions retryOptions = RetryOptions.of().withMaxAttempts(2).withIntervalSeconds(0.1);
        Runnable failing = () -> { throw new RuntimeException("fail"); };

        assertThatThrownBy(() -> runner.run("step", failing, retryOptions))
                .isInstanceOf(RuntimeException.class);
    }
}
