package io.openleap.core.scheduling.dbos.workflow;

import java.util.UUID;

public interface TaskDispatchWorkflow {

    String execute(String taskId, UUID tenantId, String handlerName, String payloadJson);

}
