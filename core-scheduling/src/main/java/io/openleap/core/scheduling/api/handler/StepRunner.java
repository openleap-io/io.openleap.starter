package io.openleap.core.scheduling.api.handler;

import java.util.concurrent.Callable;

public interface StepRunner {

    <T> T run(String name, Callable<T> step);

    void run(String name, Runnable step);

    <T> T run(String name, Callable<T> step, RetryOptions retryOptions);

    void run(String name, Runnable step, RetryOptions retryOptions);
}
