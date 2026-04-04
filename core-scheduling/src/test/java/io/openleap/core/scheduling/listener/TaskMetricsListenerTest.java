package io.openleap.core.scheduling.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMetricsListenerTest {

    private static final String TASK_ID = "task-1";
    private static final String HANDLER_NAME = "audit-log";

    private SimpleMeterRegistry meterRegistry;
    private TaskMetricsListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new TaskMetricsListener(meterRegistry);
    }

    @Test
    void onSubmitted_incrementsSubmittedCounter_withHandlerTag() {
        listener.onSubmitted(TASK_ID, HANDLER_NAME);

        Counter counter = meterRegistry.find("tasks.submitted").tag("handler", HANDLER_NAME).counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void onCompleted_incrementsCompletedCounter_withHandlerTag() {
        listener.onCompleted(TASK_ID, HANDLER_NAME);

        Counter counter = meterRegistry.find("tasks.completed").tag("handler", HANDLER_NAME).counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void onFailed_incrementsFailedCounter_withHandlerAndErrorTags() {
        listener.onFailed(TASK_ID, HANDLER_NAME, new RuntimeException("timeout"));

        Counter counter = meterRegistry.find("tasks.failed")
                .tag("handler", HANDLER_NAME)
                .tag("error", "RuntimeException")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void onCancelled_incrementsCancelledCounter() {
        listener.onCancelled(TASK_ID);

        Counter counter = meterRegistry.find("tasks.cancelled").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
