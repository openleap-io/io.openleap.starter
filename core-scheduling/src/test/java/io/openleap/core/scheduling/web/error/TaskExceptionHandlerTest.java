package io.openleap.core.scheduling.web.error;

import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.scheduling.api.exception.*;
import io.openleap.core.scheduling.api.queue.TaskQueue;
import io.openleap.core.scheduling.api.queue.TaskStatus;
import io.openleap.core.scheduling.registry.TaskHandlerRegistry;
import io.openleap.core.scheduling.web.controller.TaskController;
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

import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskExceptionHandlerTest {

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
    void submit_returns404_whenHandlerNotFound() throws Exception {
        doThrow(new TaskHandlerNotFoundException("handler")).when(taskQueue).submit(any());

        mockMvc.perform(post("/api/tasks/handler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void submit_returns409_whenTaskAlreadyExists() throws Exception {
        doThrow(new TaskAlreadyExistsException("task-1", "key-1")).when(taskQueue).submit(any());

        mockMvc.perform(post("/api/tasks/handler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void submit_returns429_whenQueueFull() throws Exception {
        doThrow(new TaskQueueFullException("default")).when(taskQueue).submit(any());

        mockMvc.perform(post("/api/tasks/handler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void submit_returns400_whenPayloadIsNull() throws Exception {
        mockMvc.perform(post("/api/tasks/handler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void submitAndWait_returns500_whenTaskExecutionFails() throws Exception {
        doThrow(new TaskExecutionException("task-1", "handler", new RuntimeException("fail")))
                .when(taskQueue).submitAndWait(any());

        mockMvc.perform(post("/api/tasks/handler/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void submitAndWait_returns400_whenSerializationFails() throws Exception {
        doThrow(new TaskSerializationException("handler", new RuntimeException("parse error")))
                .when(taskQueue).submitAndWait(any());

        mockMvc.perform(post("/api/tasks/handler/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void getStatus_returns404_whenTaskNotFound() throws Exception {
        doThrow(new TaskNotFoundException(TASK_ID)).when(taskQueue).getStatus(TASK_ID);

        mockMvc.perform(get("/api/tasks/{taskId}/status", TASK_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void cancel_returns409_whenTaskNotCancellable() throws Exception {
        doThrow(new TaskNotCancellableException(TASK_ID, TaskStatus.COMPLETED))
                .when(taskQueue).cancel(TASK_ID);

        mockMvc.perform(post("/api/tasks/{taskId}/cancel", TASK_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
