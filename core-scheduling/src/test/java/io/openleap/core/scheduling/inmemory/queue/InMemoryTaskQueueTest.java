package io.openleap.core.scheduling.inmemory.queue;

import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.api.queue.TaskHandle;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskStatus;
import io.openleap.core.scheduling.api.queue.TaskSubmission;
import io.openleap.core.scheduling.inmemory.step.DirectStepRunner;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// TODO (itaseski): Add unit tests for the edge cases
@ExtendWith(MockitoExtension.class)
class InMemoryTaskQueueTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    TaskHandlerRegistry registry;
    @Mock
    CompositeTaskLifecycleListener listener;
    @Mock
    DirectStepRunner stepRunner;
    @SuppressWarnings("rawtypes")
    @Mock
    TaskHandler handler;

    private InMemoryTaskQueue queue;

    @BeforeEach
    void setUp() {
        queue = new InMemoryTaskQueue(registry, Executors.newFixedThreadPool(2), stepRunner, listener, JsonMapper.builder().build());
    }

    @Test
    void submit_returnsTaskHandle() {
        when(registry.isAbsent("test-handler")).thenReturn(false);

        var handle = queue.submit(submission());

        assertThat(handle)
                .isNotNull()
                .returns("test-handler", TaskHandle::handlerName)
                .doesNotReturn(null, TaskHandle::taskId)
                .doesNotReturn(null, TaskHandle::submittedAt);

        verify(listener).onSubmitted(any(), eq("test-handler"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitAndWait_returnsResult() {
        when(registry.isAbsent("test-handler")).thenReturn(false);
        doReturn(handler).when(registry).get("test-handler");
        when(handler.payloadType()).thenReturn(Map.class);
        when(handler.handle(any(), any())).thenReturn(Map.of("echo", "hello"));
        when(handler.resultType()).thenReturn(Map.class);

        Map<?, ?> result = queue.submitAndWait(submission());

        assertThat(result).isEqualTo(Map.of("echo", "hello"));

        verify(listener).onSubmitted(any(), eq("test-handler"));
        verify(listener).onCompleted(any(), eq("test-handler"));
    }

    @Test
    void getStatus_returnsTaskResult() {
        when(registry.isAbsent("test-handler")).thenReturn(false);

        var handle = queue.submit(submission());

        TaskResult result = queue.getStatus(handle.taskId());

        assertThat(result)
                .isNotNull()
                .returns(handle.taskId(), TaskResult::taskId);
    }

    @Test
    void cancel_setsStatusToCancelled() {
        when(registry.isAbsent("test-handler")).thenReturn(false);

        var handle = queue.submit(submission());

        queue.cancel(handle.taskId());

        assertThat(queue.getStatus(handle.taskId()).status()).isEqualTo(TaskStatus.CANCELLED);

        verify(listener).onCancelled(handle.taskId());
    }

    private TaskSubmission submission() {
        return TaskSubmission.forHandler("test-handler")
                .tenant(TENANT_ID)
                .payload(Map.of("message", "hello"))
                .build();
    }
}
