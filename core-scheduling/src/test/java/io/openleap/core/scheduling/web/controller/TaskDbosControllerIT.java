package io.openleap.core.scheduling.web.controller;

import io.openleap.core.scheduling.api.queue.TaskHandle;
import io.openleap.core.scheduling.api.queue.TaskResult;
import io.openleap.core.scheduling.api.queue.TaskStatus;
import io.openleap.core.scheduling.support.TestFixtures;
import io.openleap.core.scheduling.support.TestHandler;
import io.openleap.core.scheduling.support.TestIdentityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@TestPropertySource(properties = "task.executor=dbos")
@Testcontainers
@Import({TestIdentityFilter.class, TestHandler.class})
class TaskDbosControllerIT extends AbstractTaskControllerIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void dbosProperties(DynamicPropertyRegistry registry) {
        registry.add("dbos.jdbc-url", postgres::getJdbcUrl);
        registry.add("dbos.username", postgres::getUsername);
        registry.add("dbos.password", postgres::getPassword);
    }

    @Test
    void cancel_returns204() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TestFixtures.TENANT_HEADER, TestFixtures.TENANT_ID.toString());

        TaskHandle handle = restTemplate.postForEntity(
                "/api/tasks/test-handler",
                new HttpEntity<>("{\"payload\": {\"message\": \"hello\"}}", headers),
                TaskHandle.class).getBody();

        assertThat(handle).isNotNull();

        HttpHeaders tenantHeaders = new HttpHeaders();
        tenantHeaders.set(TestFixtures.TENANT_HEADER, TestFixtures.TENANT_ID.toString());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/tasks/{taskId}/cancel", HttpMethod.POST,
                new HttpEntity<>(tenantHeaders), Void.class, handle.taskId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<TaskResult> statusResponse = restTemplate.exchange(
                "/api/tasks/{taskId}/status", HttpMethod.GET,
                new HttpEntity<>(tenantHeaders), TaskResult.class, handle.taskId());

        assertThat(statusResponse.getBody())
                .isNotNull()
                .returns(TaskStatus.CANCELLED, TaskResult::status);
    }

}