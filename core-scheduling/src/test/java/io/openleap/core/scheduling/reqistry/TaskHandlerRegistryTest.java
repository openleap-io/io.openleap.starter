package io.openleap.core.scheduling.reqistry;

import io.openleap.core.scheduling.api.exception.TaskHandlerNotFoundException;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskHandlerRegistryTest {

    private TaskHandler<?, ?> auditLog;
    private TaskHandler<?, ?> reportGenerate;
    private TaskHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        auditLog = mockHandler("audit-log");
        reportGenerate = mockHandler("report-generate");
        registry = new TaskHandlerRegistry(List.of(auditLog, reportGenerate));
    }

    @Test
    void get_returnsHandler_whenRegistered() {
        assertThat(registry.get("audit-log")).isSameAs(auditLog);
    }

    @Test
    void get_throwsTaskHandlerNotFoundException_whenNotRegistered() {
        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(TaskHandlerNotFoundException.class);
    }

    @Test
    void contains_returnsTrue_whenRegistered() {
        assertThat(registry.contains("audit-log")).isTrue();
    }

    @Test
    void contains_returnsFalse_whenNotRegistered() {
        assertThat(registry.contains("unknown")).isFalse();
    }

    @Test
    void isAbsent_returnsTrue_whenNotRegistered() {
        assertThat(registry.isAbsent("unknown")).isTrue();
    }

    @Test
    void isAbsent_returnsFalse_whenRegistered() {
        assertThat(registry.isAbsent("audit-log")).isFalse();
    }

    @Test
    void all_returnsAllRegisteredHandlers() {
        assertThat(registry.all()).containsExactlyInAnyOrder(auditLog, reportGenerate);
    }

    private TaskHandler<?, ?> mockHandler(String name) {
        TaskHandler<?, ?> handler = mock(TaskHandler.class);
        when(handler.name()).thenReturn(name);
        return handler;
    }
}
