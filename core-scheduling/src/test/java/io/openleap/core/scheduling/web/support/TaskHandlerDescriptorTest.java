package io.openleap.core.scheduling.web.support;

import io.openleap.core.scheduling.api.handler.NoPayload;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.web.dto.HandlerInfoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskHandlerDescriptorTest {

    record AuditLogPayload(String entityId, String action) {}
    record AuditLogResult(String auditId) {}

    @Test
    void describe_mapsAllFields() {
        TaskHandler<?, ?> handler = mockHandler("audit-log", AuditLogPayload.class, AuditLogResult.class);

        HandlerInfoResponse response = TaskHandlerDescriptor.describe(handler);

        assertThat(response.name()).isEqualTo("audit-log");
        assertThat(response.payloadType()).isEqualTo("AuditLogPayload");
        assertThat(response.payloadFields()).containsEntry("entityId", "String").containsEntry("action", "String");
        assertThat(response.resultType()).isEqualTo("AuditLogResult");
        assertThat(response.resultFields()).containsEntry("auditId", "String");
    }

    @Test
    void describe_returnsEmptyResultFields_whenResultTypeIsVoid() {
        TaskHandler<?, ?> handler = mockHandler("audit-log", NoPayload.class, Void.class);

        HandlerInfoResponse response = TaskHandlerDescriptor.describe(handler);

        assertThat(response.payloadType()).isEqualTo("NoPayload");
        assertThat(response.payloadFields()).isEmpty();
        assertThat(response.resultType()).isEqualTo("Void");
        assertThat(response.resultFields()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private <P, R> TaskHandler<P, R> mockHandler(String name, Class<P> payloadType, Class<R> resultType) {
        TaskHandler<P, R> handler = mock(TaskHandler.class);
        when(handler.name()).thenReturn(name);
        when(handler.payloadType()).thenReturn(payloadType);
        when(handler.resultType()).thenReturn(resultType);
        return handler;
    }
}
