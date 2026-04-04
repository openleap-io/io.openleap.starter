package io.openleap.core.scheduling.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.openleap.core.scheduling.api.listener.TaskLifecycleListener;

// TODO (itaseski): Consider adding duration time metrics for tasks i.e time between
//  onSubmitted and onCompleted/onFailed
public class TaskMetricsListener implements TaskLifecycleListener {

    private static final String HANDLER_TAG = "handler";

    private static final String ERROR_TAG = "error";

    private final MeterRegistry meterRegistry;

    public TaskMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onSubmitted(String taskId, String handlerName) {
        Counter.builder("tasks.submitted")
                .tag(HANDLER_TAG, handlerName)
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void onCompleted(String taskId, String handlerName) {
        Counter.builder("tasks.completed")
                .tag(HANDLER_TAG, handlerName)
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void onFailed(String taskId, String handlerName, Throwable error) {
        Counter.builder("tasks.failed")
                .tag(HANDLER_TAG, handlerName)
                .tag(ERROR_TAG, error.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }

    // TODO (itaseski): Its a bit tricky to retrieve handler on task cancel so we only track total cancelled
    //  count for now, but we can consider adding handler tag in the future if needed
    @Override
    public void onCancelled(String taskId) {
        Counter.builder("tasks.cancelled")
                .register(meterRegistry)
                .increment();
    }
}
