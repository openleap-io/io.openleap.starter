package io.openleap.core.scheduling.listener;

import io.openleap.core.scheduling.api.listener.TaskLifecycleListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompositeTaskLifecycleListenerTest {

    private static final String TASK_ID = "task-1";
    private static final String HANDLER_NAME = "audit-log";

    @Mock
    private TaskLifecycleListener first;
    @Mock
    private TaskLifecycleListener second;

    private CompositeTaskLifecycleListener composite;

    @BeforeEach
    void setUp() {
        composite = new CompositeTaskLifecycleListener(List.of(first, second));
    }

    @Test
    void onSubmitted_delegatesToAllListeners() {
        composite.onSubmitted(TASK_ID, HANDLER_NAME);

        verify(first, times(1)).onSubmitted(TASK_ID, HANDLER_NAME);
        verify(second, times(1)).onSubmitted(TASK_ID, HANDLER_NAME);
    }

    @Test
    void onCompleted_delegatesToAllListeners() {
        composite.onCompleted(TASK_ID, HANDLER_NAME);

        verify(first, times(1)).onCompleted(TASK_ID, HANDLER_NAME);
        verify(second, times(1)).onCompleted(TASK_ID, HANDLER_NAME);
    }

    @Test
    void onFailed_delegatesToAllListeners() {
        RuntimeException error = new RuntimeException("timeout");
        composite.onFailed(TASK_ID, HANDLER_NAME, error);

        verify(first, times(1)).onFailed(TASK_ID, HANDLER_NAME, error);
        verify(second, times(1)).onFailed(TASK_ID, HANDLER_NAME, error);
    }

    @Test
    void onCancelled_delegatesToAllListeners() {
        composite.onCancelled(TASK_ID);

        verify(first, times(1)).onCancelled(TASK_ID);
        verify(second, times(1)).onCancelled(TASK_ID);
    }
}
