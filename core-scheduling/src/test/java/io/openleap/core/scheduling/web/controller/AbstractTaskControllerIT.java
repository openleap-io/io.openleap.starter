package io.openleap.core.scheduling.web.controller;

import io.openleap.core.scheduling.api.queue.TaskHandle;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskStatus;
import io.openleap.core.scheduling.support.TestFixtures;
import io.openleap.core.scheduling.web.dto.TaskSyncResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractTaskControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void submit_returns202_withTaskHandle() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TestFixtures.TENANT_HEADER, TestFixtures.TENANT_ID.toString());

        HttpEntity<String> request = new HttpEntity<>("{\"payload\": {\"message\": \"hello\"}}", headers);

        ResponseEntity<TaskHandle> response = restTemplate.postForEntity(
                "/api/tasks/test-handler", request, TaskHandle.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody())
                .isNotNull()
                .returns("test-handler", TaskHandle::handlerName)
                .doesNotReturn(null, TaskHandle::taskId)
                .doesNotReturn(null, TaskHandle::submittedAt);
    }

    @Test
    void submitAndWait_returns200_withResult() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TestFixtures.TENANT_HEADER, TestFixtures.TENANT_ID.toString());

        HttpEntity<String> request = new HttpEntity<>("{\"payload\": {\"message\": \"hello\"}}", headers);

        ResponseEntity<TaskSyncResponse> response = restTemplate.postForEntity(
                "/api/tasks/test-handler/sync", request, TaskSyncResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(Map.of("echo", "hello"), TaskSyncResponse::result);
    }

    @Test
    void getStatus_returns200_withTaskResult() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TestFixtures.TENANT_HEADER, TestFixtures.TENANT_ID.toString());

        TaskHandle handle = restTemplate.postForEntity(
                "/api/tasks/test-handler",
                new HttpEntity<>("{\"payload\": {\"message\": \"hello\"}}", headers),
                TaskHandle.class).getBody();

        HttpHeaders tenantHeaders = new HttpHeaders();
        tenantHeaders.set(TestFixtures.TENANT_HEADER, TestFixtures.TENANT_ID.toString());

        ResponseEntity<TaskResult> response = restTemplate.exchange(
                "/api/tasks/{taskId}/status", HttpMethod.GET,
                new HttpEntity<>(tenantHeaders), TaskResult.class, handle.taskId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(handle.taskId(), TaskResult::taskId)
                .doesNotReturn(TaskStatus.UNKNOWN, TaskResult::status);
    }

}