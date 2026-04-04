package io.openleap.core.scheduling.dbos.workflow;

import io.openleap.core.scheduling.api.exception.TaskExecutionException;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.dbos.step.DbosStepRunner;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class TaskDispatchWorkflowImplTest {

    @Mock
    private TaskHandlerRegistry registry;
    @Mock
    private DbosStepRunner stepRunner;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CompositeTaskLifecycleListener listener;

    private TaskDispatchWorkflowImpl impl;

    @BeforeEach
    void setUp() {
        impl = new TaskDispatchWorkflowImpl(registry, stepRunner, objectMapper, listener);
    }

    private static final String TASK_ID = "tenant-1_abc";
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String HANDLER_NAME = "handler";
    private static final String PAYLOAD_JSON = "{\"key\":\"value\"}";

    @Test
    void execute_deserializesPayloadCallsHandlerAndReturnsResultJson() throws Exception {
        TaskHandler<Object, Object> handler = mock(TaskHandler.class);
        Object payload = new Object();
        Object result = new Object();

        doReturn(handler).when(registry).get(HANDLER_NAME);
        when(handler.payloadType()).thenReturn(Object.class);
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);
        when(handler.handle(payload, stepRunner)).thenReturn(result);
        when(objectMapper.writeValueAsString(result)).thenReturn("{\"result\":\"ok\"}");

        String resultJson = impl.execute(TASK_ID, TENANT_ID, HANDLER_NAME, PAYLOAD_JSON);

        assertEquals("{\"result\":\"ok\"}", resultJson);
        verify(listener, times(1)).onCompleted(TASK_ID, HANDLER_NAME);
    }

    @Test
    void execute_returnsNull_whenHandlerReturnsNull() throws Exception {
        TaskHandler<Object, Object> handler = mock(TaskHandler.class);
        Object payload = new Object();

        doReturn(handler).when(registry).get(HANDLER_NAME);
        when(handler.payloadType()).thenReturn(Object.class);
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);
        when(handler.handle(payload, stepRunner)).thenReturn(null);

        String resultJson = impl.execute(TASK_ID, TENANT_ID, HANDLER_NAME, PAYLOAD_JSON);

        assertNull(resultJson);
        verify(listener, times(1)).onCompleted(TASK_ID, HANDLER_NAME);
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void execute_callsOnFailed_andRethrows_whenTaskExecutionExceptionThrown() throws Exception {
        TaskHandler<Object, Object> handler = mock(TaskHandler.class);
        Object payload = new Object();
        TaskExecutionException exception = new TaskExecutionException(TASK_ID, HANDLER_NAME, new RuntimeException("fail"));

        doReturn(handler).when(registry).get(HANDLER_NAME);
        when(handler.payloadType()).thenReturn(Object.class);
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);
        when(handler.handle(payload, stepRunner)).thenThrow(exception);

        TaskExecutionException thrown = assertThrows(TaskExecutionException.class,
                () -> impl.execute(TASK_ID, TENANT_ID, HANDLER_NAME, PAYLOAD_JSON));

        assertSame(exception, thrown);
        verify(listener, times(1)).onFailed(TASK_ID, HANDLER_NAME, exception);
        verify(listener, never()).onCompleted(any(), any());
    }

    @Test
    void execute_wrapsException_andCallsOnFailed_whenGenericExceptionThrown() throws Exception {
        TaskHandler<Object, Object> handler = mock(TaskHandler.class);
        Object payload = new Object();
        RuntimeException cause = new RuntimeException("unexpected");

        doReturn(handler).when(registry).get(HANDLER_NAME);
        when(handler.payloadType()).thenReturn(Object.class);
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);
        when(handler.handle(payload, stepRunner)).thenThrow(cause);

        TaskExecutionException thrown = assertThrows(TaskExecutionException.class,
                () -> impl.execute(TASK_ID, TENANT_ID, HANDLER_NAME, PAYLOAD_JSON));

        assertSame(cause, thrown.getCause());
        verify(listener, times(1)).onFailed(eq(TASK_ID), eq(HANDLER_NAME), any(TaskExecutionException.class));
        verify(listener, never()).onCompleted(any(), any());
    }
}
