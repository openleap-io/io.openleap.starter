package io.openleap.core.scheduling.iam;

import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.scheduling.api.exception.TaskNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskAuthorizationServiceTest {

    private TaskAuthorizationService service;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        service = new TaskAuthorizationService();
        IdentityHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        IdentityHolder.clear();
    }

    @Test
    void hasAccess_doesNotThrow_whenTaskIdBelongsToCurrentTenant() {
        assertDoesNotThrow(() -> service.hasAccess(TENANT_ID + "_abc"));
    }

    @Test
    void hasAccess_throwsTaskNotFoundException_whenTaskIdBelongsToDifferentTenant() {
        String otherTenantTaskId = UUID.randomUUID() + "_abc";

        assertThrows(TaskNotFoundException.class, () -> service.hasAccess(otherTenantTaskId));
    }

    @Test
    void tenantPrefix_returnsTenantIdFollowedByUnderscore() {
        assertEquals(TENANT_ID + "_", service.tenantPrefix());
    }
}
