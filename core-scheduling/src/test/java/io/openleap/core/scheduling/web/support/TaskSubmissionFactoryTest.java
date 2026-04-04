package io.openleap.core.scheduling.web.support;

import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.scheduling.api.queue.TaskSubmission;
import io.openleap.core.scheduling.web.dto.TaskSubmitRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSubmissionFactoryTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        IdentityHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        IdentityHolder.clear();
    }

    @Test
    void from_mapsAllFields() {
        TaskSubmitRequest request = new TaskSubmitRequest(
                JsonNodeFactory.instance.objectNode(), "dedup-key", 2, 30L);

        TaskSubmission submission = TaskSubmissionFactory.from("audit-log", request);

        assertThat(submission.getHandlerName()).isEqualTo("audit-log");
        assertThat(submission.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(submission.getPayload()).isEqualTo(request.payload());
        assertThat(submission.getDeduplicationKey()).isEqualTo("dedup-key");
        assertThat(submission.getPriority()).isEqualTo(2);
        assertThat(submission.getTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void from_leavesOptionalFieldsNull_whenAbsent() {
        TaskSubmitRequest request = new TaskSubmitRequest(JsonNodeFactory.instance.objectNode(), null, null, null);

        TaskSubmission submission = TaskSubmissionFactory.from("audit-log", request);

        assertThat(submission.getDeduplicationKey()).isNull();
        assertThat(submission.getPriority()).isNull();
        assertThat(submission.getTimeout()).isNull();
    }
}
