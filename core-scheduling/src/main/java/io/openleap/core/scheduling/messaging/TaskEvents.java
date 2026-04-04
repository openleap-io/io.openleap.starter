package io.openleap.core.scheduling.messaging;

public final class TaskEvents {

    public static final String AGGREGATE_TYPE = "Task";

    public static final String SUBMITTED = "Submitted";
    public static final String COMPLETED = "Completed";
    public static final String FAILED    = "Failed";
    public static final String CANCELLED = "Cancelled";

    public static final String ROUTING_SUBMITTED = ".task.submitted";
    public static final String ROUTING_COMPLETED = ".task.completed";
    public static final String ROUTING_FAILED    = ".task.failed";
    public static final String ROUTING_CANCELLED = ".task.cancelled";

    public static final String META_HANDLER = "handler";
    public static final String META_CAUSE   = "cause";

    private TaskEvents() {

    }
}
