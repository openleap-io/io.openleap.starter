package io.openleap.core.scheduling.inmemory.step;

import io.openleap.core.scheduling.api.handler.RetryOptions;
import io.openleap.core.scheduling.api.handler.StepRunner;

import java.util.concurrent.Callable;

// TODO (itaseski): Too much exception wrapping leading to noisy exceptions and nesting the original one
public class DirectStepRunner implements StepRunner {

    @Override
    public <T> T run(String name, Callable<T> step) {
        try {
            return step.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(String name, Runnable step) {
        step.run();
    }

    @Override
    public <T> T run(String name, Callable<T> step, RetryOptions retryOptions) {
        try {
            return RetryExecutor.execute(name, step, retryOptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(String name, Runnable step, RetryOptions retryOptions) {
        try {
            RetryExecutor.execute(name, () -> {
                step.run();
                return null;
            }, retryOptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
