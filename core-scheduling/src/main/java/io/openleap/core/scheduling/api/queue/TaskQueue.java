package io.openleap.core.scheduling.api.queue;

public interface TaskQueue {

    TaskHandle submit(TaskSubmission request);

    <R> R submitAndWait(TaskSubmission submission);

    // TODO (itaseski): Add support for result retrieval as part of getStatus or getResult
    TaskResult getStatus(String taskId);

    void cancel(String taskId);

    // TODO (itaseski): Add support for listing tasks per handler, status, from/to
}
