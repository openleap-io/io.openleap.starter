package io.openleap.core.scheduling.dbos.queue;

import dev.dbos.transact.workflow.ErrorResult;
import dev.dbos.transact.workflow.WorkflowState;
import dev.dbos.transact.workflow.WorkflowStatus;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DbosMapperTest {

    private final DbosMapper mapper = Mappers.getMapper(DbosMapper.class);

    @Test
    void toTaskResult_returnsUnknown_whenStatusIsNull() {
        TaskResult result = mapper.toTaskResult("task-1", null);

        assertEquals("task-1", result.taskId());
        assertEquals(TaskStatus.UNKNOWN, result.status());
    }

    @Test
    void toTaskResult_mapsFieldsCorrectly() {
        TaskResult result = mapper.toTaskResult("task-1", mockStatus("PENDING", null));

        assertEquals("task-1", result.taskId());
        assertEquals(TaskStatus.RUNNING, result.status());
        assertEquals(Instant.ofEpochMilli(1000L), result.submittedAt());
        assertEquals(Instant.ofEpochMilli(2000L), result.startedAt());
        assertNull(result.completedAt());
    }

    @Test
    void toTaskResult_setsCompletedAt_whenTerminal() {
        TaskResult result = mapper.toTaskResult("task-1", mockStatus("SUCCESS", null));

        assertEquals(Instant.ofEpochMilli(3000L), result.completedAt());
    }

    @Test
    void toTaskResult_mapsError_whenPresent() {
        ErrorResult error = mock(ErrorResult.class);
        when(error.className()).thenReturn("IllegalStateException");
        when(error.message()).thenReturn("something went wrong");

        TaskResult result = mapper.toTaskResult("task-1", mockStatus("ERROR", error));

        assertEquals("IllegalStateException", result.errorCode());
        assertEquals("something went wrong", result.errorMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "ENQUEUED, PENDING",
            "PENDING, RUNNING",
            "SUCCESS, COMPLETED",
            "ERROR, FAILED",
            "MAX_RECOVERY_ATTEMPTS_EXCEEDED, FAILED",
            "CANCELLED, CANCELLED"
    })
    void toTaskStatus_mapsCorrectly(WorkflowState workflowState, TaskStatus expectedStatus) {
        assertEquals(expectedStatus, mapper.toTaskStatus(workflowState));
    }

    private WorkflowStatus mockStatus(String state, ErrorResult error) {
        WorkflowStatus status = mock(WorkflowStatus.class);
        when(status.status()).thenReturn(state);
        when(status.createdAt()).thenReturn(1000L);
        when(status.startedAtEpochMs()).thenReturn(2000L);
        when(status.updatedAt()).thenReturn(3000L);
        when(status.error()).thenReturn(error);
        return status;
    }
}
