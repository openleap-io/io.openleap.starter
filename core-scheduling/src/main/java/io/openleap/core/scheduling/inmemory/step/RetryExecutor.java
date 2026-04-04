package io.openleap.core.scheduling.inmemory.step;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.openleap.core.scheduling.api.handler.RetryOptions;

import java.time.Duration;
import java.util.concurrent.Callable;

// TODO (itaseski): Consider introducing more advanced circuit breaker in the future.
public class RetryExecutor {

    private RetryExecutor() {
    }

    public static <T> T execute(String name, Callable<T> step, RetryOptions retryOptions) throws Exception {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryOptions.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis((long) (retryOptions.intervalSeconds() * 1000)),
                        retryOptions.backoffRate()))
                .build();

        return Retry.of(name, config).executeCallable(step);
    }
}
