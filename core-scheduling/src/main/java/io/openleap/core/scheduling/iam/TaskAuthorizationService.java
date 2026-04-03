package io.openleap.core.scheduling.iam;

import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.scheduling.api.exception.TaskNotFoundException;

import java.util.UUID;

public class TaskAuthorizationService {

    // TODO (itaseski): Consider returning a boolean instead of throwing an exception
    public void hasAccess(String taskId) {
        UUID tenantId = IdentityHolder.getTenantId();
        if (!taskId.startsWith(tenantId + "_")) {
            throw new TaskNotFoundException(taskId);
        }
    }

    public String tenantPrefix() {
        return IdentityHolder.getTenantId() + "_";
    }
}
