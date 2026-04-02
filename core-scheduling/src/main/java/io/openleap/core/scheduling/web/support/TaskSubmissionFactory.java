package io.openleap.core.scheduling.web.support;

import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.scheduling.api.queue.TaskSubmission;
import io.openleap.core.scheduling.web.dto.TaskSubmitRequest;

import java.time.Duration;

public class TaskSubmissionFactory {

    private TaskSubmissionFactory() {

    }

    public static TaskSubmission from(String handler, TaskSubmitRequest request) {
        TaskSubmission.Builder builder = TaskSubmission
                .forHandler(handler)
                .tenant(IdentityHolder.getTenantId())
                .payload(request.payload());

        if (request.deduplicationKey() != null) {
            builder.deduplicationKey(request.deduplicationKey());
        }
        if (request.priority() != null) {
            builder.priority(request.priority());
        }
        if (request.timeoutSeconds() != null) {
            builder.timeout(Duration.ofSeconds(request.timeoutSeconds()));
        }

        return builder.build();
    }

}
