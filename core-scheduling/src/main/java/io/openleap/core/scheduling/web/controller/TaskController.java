package io.openleap.core.scheduling.web.controller;

import io.openleap.core.scheduling.api.queue.TaskHandle;
import io.openleap.core.scheduling.api.queue.TaskQueue;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskSubmission;
import io.openleap.core.scheduling.iam.AuthorizeTenantAccess;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import io.openleap.core.scheduling.web.dto.HandlerInfoResponse;
import io.openleap.core.scheduling.web.dto.TaskSubmitRequest;
import io.openleap.core.scheduling.web.dto.TaskSyncResponse;
import io.openleap.core.scheduling.web.support.TaskHandlerDescriptor;
import io.openleap.core.scheduling.web.support.TaskSubmissionFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

// TODO (itaseski): Point of improvement: have scheduling as an independent service
//  and handlers registered in independent services
// TODO (itaseski): Add role based authorization
@Tag(name = "Tasks", description = "Task submission, status retrieval and cancellation")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskQueue taskQueue;
    private final TaskHandlerRegistry registry;

    public TaskController(TaskQueue taskQueue, TaskHandlerRegistry registry) {
        this.taskQueue = taskQueue;
        this.registry = registry;
    }

    @Operation(summary = "Submit a task", description = "Submits a task to the specified handler and returns immediately with a task handle")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Task accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Handler not found"),
            @ApiResponse(responseCode = "409", description = "Task with deduplication key already exists")
    })
    @PostMapping("/{handler}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskHandle submitTask(
            @Parameter(description = "Name of the registered handler to execute", required = true) @PathVariable String handler,
            @RequestBody @Valid TaskSubmitRequest request) {
        TaskSubmission taskSubmission = TaskSubmissionFactory.from(handler, request);
        return taskQueue.submit(taskSubmission);
    }

    @Operation(summary = "Submit a task and wait for result", description = "Submits a task to the specified handler and blocks until execution completes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Handler not found"),
            @ApiResponse(responseCode = "409", description = "Task with deduplication key already exists"),
            @ApiResponse(responseCode = "500", description = "Task execution failed")
    })
    @PostMapping("/{handler}/sync")
    public TaskSyncResponse submitAndWait(
            @Parameter(description = "Name of the registered handler to execute", required = true) @PathVariable String handler,
            @RequestBody @Valid TaskSubmitRequest request) {
        TaskSubmission taskSubmission = TaskSubmissionFactory.from(handler, request);
        Object result = taskQueue.submitAndWait(taskSubmission);
        return new TaskSyncResponse(result);
    }

    @Operation(summary = "List registered handlers", description = "Returns all task handlers registered in this service, sorted by name")
    @ApiResponse(responseCode = "200", description = "List of registered handlers")
    @GetMapping("/handlers")
    public List<HandlerInfoResponse> listHandlers() {
        return registry.all().stream()
                .map(TaskHandlerDescriptor::describe)
                .sorted(Comparator.comparing(HandlerInfoResponse::name))
                .toList();
    }

    // TODO (itaseski) Add support for result retrieval as part of getStatus or new getResult endpoint

    @Operation(summary = "Get task status", description = "Returns the current status of a task. Only accessible by the tenant that submitted it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task status retrieved"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
    })
    @GetMapping("/{taskId}/status")
    @AuthorizeTenantAccess
    public TaskResult getStatus(
            @Parameter(description = "ID of the task", required = true) @PathVariable String taskId) {
        return taskQueue.getStatus(taskId);
    }

    @Operation(summary = "Cancel a task", description = "Cancels a pending or running task. Only accessible by the tenant that submitted it")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task cancelled"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be cancelled in its current status")
    })
    @PostMapping("/{taskId}/cancel")
    @AuthorizeTenantAccess
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTask(
            @Parameter(description = "ID of the task to cancel", required = true) @PathVariable String taskId) {
        taskQueue.cancel(taskId);
    }

    // TODO (itaseski): add endpoint for listing tasks per handler, status, from/to
}
