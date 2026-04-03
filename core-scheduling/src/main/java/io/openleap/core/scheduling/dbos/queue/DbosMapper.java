package io.openleap.core.scheduling.dbos.queue;

import dev.dbos.transact.workflow.ErrorResult;
import dev.dbos.transact.workflow.WorkflowState;
import dev.dbos.transact.workflow.WorkflowStatus;

import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskStatus;
import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;

import java.time.Instant;

// TODO (itaseski): Consider removing MapStruct since its only used for status mapping
@Mapper(componentModel = "spring")
public interface DbosMapper {

    default TaskResult toTaskResult(String taskId, WorkflowStatus status) {
        if (status == null) {
            return TaskResult.unknown(taskId);
        }

        TaskStatus taskStatus = toTaskStatus(WorkflowState.valueOf(status.status()));
        ErrorResult error = status.error();
        boolean terminal = taskStatus == TaskStatus.COMPLETED || taskStatus == TaskStatus.FAILED;

        return new TaskResult(
                taskId,
                taskStatus,
                toInstant(status.createdAt()),
                toInstant(status.startedAtEpochMs()),
                terminal ? toInstant(status.updatedAt()) : null,
                error != null ? error.className() : null,
                error != null ? error.message() : null
        );
    }

    private static Instant toInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

    // TODO (itaseski): Double check the mappings
    @ValueMapping(source = "ENQUEUED", target = "PENDING")
    @ValueMapping(source = "PENDING", target = "RUNNING")
    @ValueMapping(source = "SUCCESS", target = "COMPLETED")
    @ValueMapping(source = "ERROR", target = "FAILED")
    @ValueMapping(source = "MAX_RECOVERY_ATTEMPTS_EXCEEDED", target = "FAILED")
    @ValueMapping(source = "CANCELLED", target = "CANCELLED")
    TaskStatus toTaskStatus(WorkflowState state);
}
