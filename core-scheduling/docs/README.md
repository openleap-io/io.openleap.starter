# core-scheduling

Durable task execution library for OpenLeap services. Provides a unified API for submitting, tracking, and cancelling async tasks — backed by [DBOS](https://dbos.dev) for durable execution or an in-memory executor for local development and testing.

---

## Status

> ⚠️ Work in progress — not ready for production use

### Done
- [x] DBOS-backed durable task execution with crash recovery
- [x] In-memory implementation for local dev and testing
- [x] Generic REST API auto-registered in every consuming service
- [x] Tenant isolation via `@AuthorizeTenantAccess`
- [x] Task lifecycle listeners (`TaskLoggingListener`, `TaskMetricsListener`, `TaskEventPublisher`)
- [x] Retry with exponential backoff (Resilience4j)
- [x] Per-handler workflow registration (visible as distinct workflows in DBOS Conductor UI)
- [x] Queue partitioning by tenant for fairness
- [x] Spring Boot auto-configuration — zero boilerplate for consumers
- [x] OpenAPI/Swagger annotations on all endpoints
- [x] Bruno collection

### Pending
- [ ] `listTasks` endpoint — filter by handler, status, from/to
- [ ] Result retrieval endpoint — `GET /{taskId}/result`
- [ ] Per-handler retry configuration
- [ ] `@TenantScoped` annotation for tenant-scoped endpoints without a `taskId`
- [ ] Fix race condition in `InMemoryTaskQueue` (`CompletableFuture`)
- [ ] Duration metrics (`tasks.duration` timer)
- [ ] Per-task-type queues for independent concurrency limits
- [ ] Payload validation after deserialization in `TaskDispatchWorkflowImpl`
- [ ] Circuit breaker in `RetryExecutor`
- [ ] Verify `DbosMapper` state mappings against DBOS specification

---

## Adding the Dependency

```xml
<dependency>
    <groupId>io.openleap.core</groupId>
    <artifactId>core-scheduling</artifactId>
</dependency>
```

Auto-configuration activates automatically — no `@Enable*` annotation needed.

---

## Implementing a TaskHandler

The only thing a consuming service needs to implement is `TaskHandler<P, R>`:

```java
@Component
public class RenderTaskHandler implements TaskHandler<RenderPayload, RenderResult> {

    @Override
    public String name() {
        return "render"; // must be unique across all handlers in the service
    }

    @Override
    public Class<RenderPayload> payloadType() {
        return RenderPayload.class;
    }

    @Override
    public Class<RenderResult> resultType() {
        return RenderResult.class;
    }

    @Override
    public RenderResult handle(RenderPayload payload, StepRunner steps) {
        byte[] template = steps.run("fetch-template", () -> templateRepo.fetch(payload.templateId()));
        byte[] rendered = steps.run("render",          () -> renderer.render(template, payload.data()));
        String artifactId = steps.run("store",         () -> storage.store(rendered));
        return new RenderResult(artifactId);
    }
}
```

- Each operation wrapped in `steps.run(name, ...)` is a **checkpointed step** — if the service crashes, execution resumes from the last completed step.
- `name()` is used as the workflow instance name in DBOS — keep it stable across deployments.
- `payloadType()` and `resultType()` drive JSON deserialization — must be Jackson-serializable.

---

## REST API

Every service that includes `core-scheduling` automatically gets these endpoints:

| Method   | Path                       | Description                              |
|----------|----------------------------|------------------------------------------|
| `POST`   | `/api/tasks/{handler}`     | Submit a task (async, returns immediately) |
| `POST`   | `/api/tasks/{handler}/sync`| Submit a task and wait for result        |
| `GET`    | `/api/tasks/{taskId}/status` | Get task status                        |
| `DELETE` | `/api/tasks/{taskId}`      | Cancel a task                            |
| `GET`    | `/api/tasks/handlers`      | List all registered handlers             |

### Submit request body

```json
{
  "payload": {},
  "deduplicationKey": "optional-idempotency-key",
  "priority": 1,
  "timeoutSeconds": 300
}
```

### Task status response

```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000/3fa85f64",
  "status": "COMPLETED",
  "submittedAt": "2026-01-01T10:00:00Z",
  "startedAt": "2026-01-01T10:00:01Z",
  "completedAt": "2026-01-01T10:00:05Z",
  "errorCode": null,
  "errorMessage": null
}
```

Possible status values: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`, `UNKNOWN`

---

## Configuration

### Executor

```yaml
task:
  executor: dbos       # default — durable execution via DBOS
  # executor: in-memory  # lightweight — for local dev and tests
```

### DBOS (default executor)

```yaml
dbos:
  jdbc-url: jdbc:postgresql://localhost:5432/mydb
  username: postgres
  password: secret
  admin-server-enabled: false
  admin-server-port: 3001

task:
  queue:
    name: task-queue
    concurrency: 10
    worker-concurrency: 3
```

### In-memory executor

```yaml
task:
  in-memory:
    executor-type: FIXED   # FIXED | CACHED | VIRTUAL
    thread-pool-size: 4    # defaults to available processors
```

### Retry

```yaml
task:
  retry:
    max-attempts: 3
    interval-seconds: 1.0
    backoff-rate: 2.0
```

### Web layer

```yaml
task:
  web:
    enabled: true   # set to false to disable the auto-registered controller
```

---

## Lifecycle Listeners

Implement `TaskLifecycleListener` to hook into task lifecycle events:

```java
@Component
public class MyListener implements TaskLifecycleListener {

    @Override
    public void onSubmitted(String taskId, String handlerName) {}

    @Override
    public void onCompleted(String taskId, String handlerName) {}

    @Override
    public void onFailed(String taskId, String handlerName, Throwable error) {}

    @Override
    public void onCancelled(String taskId) {}
}
```

Built-in listeners registered automatically:

| Listener               | Condition                         | Purpose                         |
|------------------------|-----------------------------------|---------------------------------|
| `TaskLoggingListener`  | Always                            | Logs all lifecycle events       |
| `TaskMetricsListener`  | `MeterRegistry` on classpath      | Micrometer counters per handler |
| `TaskEventPublisher`   | `EventPublisher` bean present     | Publishes domain events via core-messaging |

---

## Tenant Isolation

Task IDs are namespaced by tenant: `{tenantId}/{uuid}`. The `@AuthorizeTenantAccess` annotation on `getStatus` and `cancel` ensures a tenant can only access their own tasks.

```java
@GetMapping("/{taskId}/status")
@AuthorizeTenantAccess  // validates taskId starts with current tenant's ID
public TaskResult getStatus(@PathVariable String taskId) { ... }
```

Tenant ID is resolved from `IdentityHolder` — no need to pass it explicitly in requests.
