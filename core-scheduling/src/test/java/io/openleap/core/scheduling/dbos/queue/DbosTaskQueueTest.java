package io.openleap.core.scheduling.dbos.queue;

import dev.dbos.transact.DBOS;
import dev.dbos.transact.execution.ThrowingRunnable;
import dev.dbos.transact.workflow.Queue;
import dev.dbos.transact.workflow.WorkflowHandle;
import dev.dbos.transact.workflow.WorkflowStatus;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.api.queue.TaskHandle;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskSubmission;
import io.openleap.core.scheduling.dbos.workflow.TaskDispatchWorkflow;
import io.openleap.core.scheduling.listener.CompositeTaskLifecycleListener;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// TODO (itaseski): Add unit tests for the edge cases
@ExtendWith(MockitoExtension.class)
class DbosTaskQueueTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    TaskHandlerRegistry registry;
    @Mock
    CompositeTaskLifecycleListener listener;
    @Mock
    DbosMapper dbosMapper;
    @Mock
    TaskDispatchWorkflow workflowProxy;
    @Mock
    Queue queue;
    @SuppressWarnings("rawtypes")
    @Mock
    TaskHandler handler;

    private DbosTaskQueue dbosTaskQueue;

    @BeforeEach
    void setUp() {
        dbosTaskQueue = new DbosTaskQueue(Map.of("test-handler", workflowProxy), registry, queue,
                JsonMapper.builder().build(), listener, dbosMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void submit_returnsTaskHandle() {
        when(registry.isAbsent("test-handler")).thenReturn(false);

        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.startWorkflow(any(ThrowingRunnable.class), any())).thenReturn(mock(WorkflowHandle.class));

            TaskHandle handle = dbosTaskQueue.submit(submission());

            assertThat(handle)
                    .isNotNull()
                    .returns("test-handler", TaskHandle::handlerName)
                    .doesNotReturn(null, TaskHandle::taskId)
                    .doesNotReturn(null, TaskHandle::submittedAt);

            verify(listener, times(1)).onSubmitted(any(), eq("test-handler"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitAndWait_returnsResult() throws Exception {
        when(registry.isAbsent("test-handler")).thenReturn(false);
        doReturn(handler).when(registry).get("test-handler");
        when(handler.resultType()).thenReturn(Map.class);

        WorkflowHandle<String, Exception> wfHandle = mock(WorkflowHandle.class);
        when(wfHandle.getResult()).thenReturn("{\"echo\":\"hello\"}");

        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.startWorkflow(any(ThrowingRunnable.class), any())).thenReturn(mock(WorkflowHandle.class));
            dbos.when(() -> DBOS.retrieveWorkflow(any())).thenReturn(wfHandle);

            Map<?, ?> result = dbosTaskQueue.submitAndWait(submission());

            assertThat(result).isEqualTo(Map.of("echo", "hello"));

            verify(listener, times(1)).onSubmitted(any(), eq("test-handler"));
        }
    }

    @Test
    void getStatus_returnsTaskResult() {
        String taskId = TENANT_ID + "_some-task-id";
        TaskResult expected = TaskResult.pending(taskId, Instant.now());

        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.getWorkflowStatus(taskId)).thenReturn(mock(WorkflowStatus.class));
            when(dbosMapper.toTaskResult(eq(taskId), any())).thenReturn(expected);

            TaskResult result = dbosTaskQueue.getStatus(taskId);

            assertThat(result)
                    .isNotNull()
                    .returns(taskId, TaskResult::taskId)
                    .doesNotReturn(null, TaskResult::status);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancel_cancelsWorkflow() {
        when(registry.isAbsent("test-handler")).thenReturn(false);

        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(() -> DBOS.startWorkflow(any(ThrowingRunnable.class), any())).thenReturn(mock(WorkflowHandle.class));

            TaskHandle handle = dbosTaskQueue.submit(submission());

            TaskResult pending = TaskResult.pending(handle.taskId(), Instant.now());
            dbos.when(() -> DBOS.getWorkflowStatus(handle.taskId())).thenReturn(mock(WorkflowStatus.class));
            when(dbosMapper.toTaskResult(eq(handle.taskId()), any())).thenReturn(pending);

            dbosTaskQueue.cancel(handle.taskId());

            dbos.verify(() -> DBOS.cancelWorkflow(handle.taskId()), times(1));
            verify(listener, times(1)).onCancelled(handle.taskId());
        }
    }

    private TaskSubmission submission() {
        return TaskSubmission.forHandler("test-handler")
                .tenant(TENANT_ID)
                .payload(Map.of("message", "hello"))
                .build();
    }
}
