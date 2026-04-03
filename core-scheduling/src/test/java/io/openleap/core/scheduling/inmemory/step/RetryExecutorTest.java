package io.openleap.core.scheduling.inmemory.step;

import io.openleap.core.scheduling.api.handler.RetryOptions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryExecutorTest {

    private static final RetryOptions RETRY_OPTIONS = RetryOptions.of()
            .withMaxAttempts(3)
            .withIntervalSeconds(0.1);

    @Test
    void execute_returnsResult_whenStepSucceeds() throws Exception {
        String result = RetryExecutor.execute("step", () -> "done", RETRY_OPTIONS);

        assertThat(result).isEqualTo("done");
    }

    @Test
    void execute_retriesAndSucceeds_whenStepFailsTransiently() throws Exception {
        int[] attempts = {0};
        Callable<String> step = () -> {
            if (++attempts[0] < 3) {
                throw new Exception("transient");
            }
            return "done";
        };

        String result = RetryExecutor.execute("step", step, RETRY_OPTIONS);

        assertThat(result).isEqualTo("done");
        assertThat(attempts[0]).isEqualTo(3);
    }

    @Test
    void execute_throwsException_afterExactlyMaxAttempts_whenAlwaysFailing() {
        int[] attempts = {0};
        Callable<Void> failing = () -> {
            ++attempts[0];
            throw new Exception("fail");
        };

        assertThatThrownBy(() -> RetryExecutor.execute("step", failing, RETRY_OPTIONS))
                .isInstanceOf(Exception.class);

        assertThat(attempts[0]).isEqualTo(3);
    }
}
