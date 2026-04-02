package io.openleap.core.scheduling.web.controller;

import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.scheduling.api.handler.TaskHandler;
import io.openleap.core.scheduling.api.queue.TaskHandle;
import io.openleap.core.scheduling.api.queue.TaskQueue;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import io.openleap.core.scheduling.web.error.TaskExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskQueue taskQueue;
    @Mock
    private TaskHandlerRegistry registry;

    private MockMvc mockMvc;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String TASK_ID = "task-1";
    private static final String VALID_BODY = "{\"payload\": {}}";

    @BeforeEach
    void setUp() {
        IdentityHolder.setTenantId(TENANT_ID);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskController(taskQueue, registry))
                .setControllerAdvice(new TaskExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @AfterEach
    void tearDown() {
        IdentityHolder.clear();
    }

    @Test
    void submit_returns202_withTaskHandle() throws Exception {
        TaskHandle handle = new TaskHandle(TASK_ID, "handler", Instant.parse("2024-01-01T00:00:00Z"));
        when(taskQueue.submit(any())).thenReturn(handle);

        mockMvc.perform(post("/api/tasks/handler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.handlerName").value("handler"));
    }

    @Test
    void submitAndWait_returns200_withResult() throws Exception {
        when(taskQueue.submitAndWait(any())).thenReturn("done");

        mockMvc.perform(post("/api/tasks/handler/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("done"));
    }

    @Test
    void getStatus_returns200_withTaskResult() throws Exception {
        TaskResult result = TaskResult.pending(TASK_ID, Instant.parse("2024-01-01T00:00:00Z"));
        when(taskQueue.getStatus(TASK_ID)).thenReturn(result);

        mockMvc.perform(get("/api/tasks/{taskId}/status", TASK_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void cancel_returns204() throws Exception {
        doNothing().when(taskQueue).cancel(TASK_ID);

        mockMvc.perform(post("/api/tasks/{taskId}/cancel", TASK_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void listHandlers_returns200_withSortedHandlers() throws Exception {
        TaskHandler<?, ?> handlerA = mockHandler("audit-log", Object.class, Void.class);
        TaskHandler<?, ?> handlerB = mockHandler("report-generate", Object.class, Void.class);
        when(registry.all()).thenReturn(List.of(handlerB, handlerA));

        mockMvc.perform(get("/api/tasks/handlers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("audit-log"))
                .andExpect(jsonPath("$[1].name").value("report-generate"));
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
