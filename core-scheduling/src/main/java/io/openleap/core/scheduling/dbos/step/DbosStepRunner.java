package io.openleap.core.scheduling.dbos.step;

import dev.dbos.transact.DBOS;
import dev.dbos.transact.workflow.StepOptions;
import io.openleap.core.scheduling.api.handler.RetryOptions;
import io.openleap.core.scheduling.api.handler.StepRunner;

import java.util.concurrent.Callable;

// TODO (itaseski): Too much wrapping leading to noisy exceptions and nesting the original one
public class DbosStepRunner implements StepRunner {

    @Override
    public <T> T run(String name, Callable<T> step) {
        return DBOS.runStep(() -> {
            try {
                return step.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, name);
    }

    @Override
    public void run(String name, Runnable step) {
        DBOS.runStep(() -> {
            step.run();
            return null;
        }, name);
    }

    @Override
    public <T> T run(String name, Callable<T> step, RetryOptions retryOptions) {
        return DBOS.runStep(() -> {
            try {
                return step.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, toStepOptions(name, retryOptions));
    }

    @Override
    public void run(String name, Runnable step, RetryOptions retryOptions) {
        DBOS.runStep(() -> {
            step.run();
            return null;
        }, toStepOptions(name, retryOptions));
    }

    private StepOptions toStepOptions(String name, RetryOptions retryOptions) {
        return new StepOptions(name)
                .withRetriesAllowed(retryOptions.maxAttempts() > 1)
                .withMaxAttempts(retryOptions.maxAttempts())
                .withIntervalSeconds(retryOptions.intervalSeconds())
                .withBackoffRate(retryOptions.backoffRate());
    }
}
