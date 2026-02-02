# OpenLeap Core-Service Starter – Features and Usage

This document describes all starter features provided by the `base/service` module (Core‑Service Starter), structured by domain. It names fully qualified classes, explains their purpose, and shows how to use them in microservices.

All examples assume your service depends on the starter module and uses Spring Boot.

## Table of contents

- Overview & quick start
- Auto‑configuration & properties
- Web/HTTP: tracing, identity, error handling
- Security & identity utilities
- Messaging: exchanges, publisher, listener, coverage
- Outbox: entities, repository, dispatcher, admin/metrics
- Repository/JPA & auditing & multi‑tenancy (RLS)
- Idempotency
- Error/response model (API)
- Utilities
- File‑by‑file overview and usage (per package)

---

## Overview & quick start

What the starter provides:

- Auto‑configuration, properties and base configs:
  - `io.openleap.starter.core.config.CoreServiceConfig`
  - `io.openleap.starter.core.config.OlStarterServiceProperties`
  - `io.openleap.starter.core.config.JacksonConfig`
  - `io.openleap.starter.core.config.OtelConfig`

- Web/HTTP infrastructure:
  - `io.openleap.starter.core.config.TraceIdFilter`
  - `io.openleap.starter.core.config.IdentityHttpFilter`
  - `io.openleap.starter.core.config.GlobalExceptionHandler`

- Security & identity:
  - `io.openleap.starter.core.security.SecurityKeycloakConfig`
  - `io.openleap.starter.core.security.SecurityLoggerConfig`
  - `io.openleap.starter.core.security.JwtUtils`
  - `io.openleap.starter.core.config.IdentityHolder`

- Messaging (RabbitMQ):
  - `io.openleap.starter.core.messaging.config.MessagingConfig`
  - `io.openleap.starter.core.messaging.RoutingKey`
  - `io.openleap.starter.core.messaging.event.OlDomainEvent`
  - `io.openleap.starter.core.messaging.event.EventPayload`
  - `io.openleap.starter.core.messaging.event.EventPublisher`
  - `io.openleap.starter.core.messaging.MessageCoverageTracker`
  - `io.openleap.starter.core.messaging.MessageCoverageReport`
  - `io.openleap.starter.core.messaging.config.MessagingIdentityPostProcessor`
  - `io.openleap.starter.core.messaging.config.MessagingIdentityClearingAdvice`
  - Commands (simple command bus):
    - `io.openleap.starter.core.messaging.command.Command`
    - `io.openleap.starter.core.messaging.command.CommandHandler`
    - `io.openleap.starter.core.messaging.command.CommandGateway`
    - `io.openleap.starter.core.messaging.command.SimpleCommandBus`

- Outbox & messaging persistence:
  - `io.openleap.starter.core.repository.entity.OlOutboxEvent`
  - `io.openleap.starter.core.repository.OutboxRepository`
  - `io.openleap.starter.core.messaging.service.OutboxOrchestrator`
  - `io.openleap.starter.core.messaging.service.OutboxAdminService`
  - `io.openleap.starter.core.messaging.service.MetricsService`

- JPA & auditing & RLS (row‑level security):
  - `io.openleap.starter.core.repository.entity.OlPersistenceEntity`
  - `io.openleap.starter.core.persistence.specification.OlSpecificationBuilder`
  - `io.openleap.starter.core.repository.config.AuditingProviderConfig`
  - `io.openleap.starter.core.repository.config.JpaAuditingConfig`
  - `io.openleap.starter.core.repository.config.TenantRlsAspect`

- Idempotency:
  - `io.openleap.starter.core.idempotency.IdempotencyRecordEntity`
  - `io.openleap.starter.core.idempotency.IdempotencyRecordRepository`
  - `io.openleap.starter.core.idempotency.IdempotencyRecordService`
  - `io.openleap.starter.core.idempotency.IdempotentReplayException`

- Error/response model (API):
  - `io.openleap.starter.core.api.error.ErrorCode`
  - `io.openleap.starter.core.api.error.ErrorResponse`
  - `io.openleap.starter.core.api.dto.OlPageableResponseDto`

- Utilities:
  - `io.openleap.starter.core.util.MoneyUtil`

Quick start (Maven):

```xml
<dependency>
  <groupId>io.openleap.starter</groupId>
  <artifactId>service</artifactId>
  <version>${openleap.starter.version}</version>
</dependency>
```

---

## Auto‑configuration & properties

- `io.openleap.starter.core.config.CoreServiceConfig` – enables configuration properties and groups core configs.

- `io.openleap.starter.core.config.OlStarterServiceProperties` – central properties under prefix `ol.service`:
  - `ol.service.security.mode` (enum `nosec`, `iamsec`): controls identity extraction for HTTP/Messaging.
  - `ol.service.messaging.events-exchange`, `ol.service.messaging.commands-exchange`
  - `ol.service.messaging.coverage` (boolean): enables runtime coverage tracking for sent messages.
  - Registry (optional, if a schema registry is available):
    - `ol.service.messaging.registry.enabled`
    - `ol.service.messaging.registry.url`
    - `ol.service.messaging.registry.format` (e.g. `avro`)
  - Outbox:
    - `ol.service.messaging.outbox.maxAttempts`
    - `ol.service.messaging.outbox.confirmTimeoutMillis`
    - `ol.service.messaging.outbox.dispatcher.enabled`
    - `ol.service.messaging.outbox.dispatcher.fixedDelay`
    - `ol.service.messaging.outbox.dispatcher.wakeupAfterCommit`
  - Metrics (optional, for queue names in metrics):
    - `ol.service.messaging.metrics.queues.main`, `ol.service.messaging.metrics.queues.dlq`

Example `application.yml`:

```yaml
ol:
  service:
    security:
      mode: iamsec
    messaging:
      events-exchange: ol.exchange.events
      commands-exchange: ol.exchange.commands
      coverage: true
      registry:
        enabled: false
      outbox:
        maxAttempts: 10
        confirmTimeoutMillis: 5000
        dispatcher:
          enabled: true
          fixedDelay: 1000
          wakeupAfterCommit: true
```

---

## Web/HTTP: tracing, identity, error handling

- `io.openleap.starter.core.config.TraceIdFilter` – sets/propagates `traceId` (MDC) per request.

- `io.openleap.starter.core.config.IdentityHttpFilter` – extracts identity from requests:
  - Mode `iamsec`: uses JWT from Spring Security context or `X-JWT` header.
  - Mode `nosec`: reads plain headers `X-Tenant-Id`, `X-User-Id`, `X-Scopes`, `X-Roles`.
  - Writes to `io.openleap.starter.core.config.IdentityHolder` and clears after completion.

- `io.openleap.starter.core.config.GlobalExceptionHandler` – unified error responses (`ErrorResponse`) for validation problems, bad requests, `ResponseStatusException`, and generic errors, using `io.openleap.starter.core.api.error.ErrorCode`.

Usage (controller example; no extra setup required):

```java
@RestController
class ExampleController {
  @GetMapping("/me")
  public Map<String, Object> me() {
    return Map.of(
      "tenantId", String.valueOf(IdentityHolder.getTenantId()),
      "userId", String.valueOf(IdentityHolder.getUserId())
    );
  }
}
```

---

## Security & identity utilities

- `io.openleap.starter.core.security.SecurityKeycloakConfig` – default resource‑server security (OAuth2/JWT) with Keycloak.
- `io.openleap.starter.core.security.SecurityLoggerConfig` – logging helpers.
- `io.openleap.starter.core.security.JwtUtils` – decode/extract claims from JWT.
- `io.openleap.starter.core.config.IdentityHolder` – thread‑local holder for `tenantId`, `userId`, `roles`, `scopes`.

Note: Mode is controlled via `ol.service.security.mode`.

---

## Messaging: exchanges, publisher, listener, coverage

Configuration/beans:

- `io.openleap.starter.core.messaging.config.MessagingConfig`
  - Beans for `TopicExchange` (events/commands)
  - `RabbitTemplate` (with optional coverage interceptor)
  - `starterRabbitListenerContainerFactory` (sets message converter, identity post‑processing, advice)
  - `starterMessageConverter`: JSON via Jackson; optionally Avro via Schema Registry (reflection based)

Identity for AMQP:

- `io.openleap.starter.core.messaging.config.MessagingIdentityPostProcessor` – extracts identity from headers (`x-tenant-id`, `x-user-id`, `x-jwt`, `x-scopes`, `x-roles`) and validates required fields.
- `io.openleap.starter.core.messaging.config.MessagingIdentityClearingAdvice` – ensures `IdentityHolder` is cleared after message processing.

Sending events:

- `io.openleap.starter.core.messaging.event.EventPayload` – base type/marker for event payloads.
- `io.openleap.starter.core.messaging.RoutingKey` – encapsulates routing keys.
- `io.openleap.starter.core.messaging.event.EventPublisher` – writes events to the Outbox transactionally and triggers the dispatcher (optionally right after commit).

Example: publish an event

```java
@Service
class OrderService {
  private final EventPublisher publisher;

  OrderService(EventPublisher publisher) { this.publisher = publisher; }

  @Transactional
  public void placeOrder(OrderCreated payload) {
    publisher.enqueue(
      RoutingKey.of("order.created"),
      payload,
      Map.of("source", "order-idempotency")
    );
  }
}
```

Coverage (optional):

- `io.openleap.starter.core.messaging.MessageCoverageTracker` – registers expected and actually sent messages; provides `MessageCoverageReport`.

Listener (consuming):

- Use the `starterRabbitListenerContainerFactory` in `@RabbitListener`:

```java
@RabbitListener(queues = "orders.queue", containerFactory = "starterRabbitListenerContainerFactory")
public void onMessage(OrderCreated event) {
  // IdentityHolder is set/validated
}
```

Commands (simple bus):

- `io.openleap.starter.core.messaging.command.Command`
- `io.openleap.starter.core.messaging.command.CommandHandler`
- `io.openleap.starter.core.messaging.command.CommandGateway`
- `io.openleap.starter.core.messaging.command.SimpleCommandBus`

---

## Outbox: entities, repository, dispatcher, admin/metrics

- `io.openleap.starter.core.repository.entity.OlOutboxEvent` – JPA entity (outbox table) with exchange, routing key, payload/headers, status.
- `io.openleap.starter.core.repository.OutboxRepository` – Spring Data repository (e.g., `findPending()`, `findByPublishedFalse()`).
- `io.openleap.starter.core.messaging.service.OutboxOrchestrator` – background dispatch from outbox to RabbitMQ with publisher confirms, retry/backoff, optional delete on ack.
- `io.openleap.starter.core.messaging.service.OutboxAdminService` – admin operations (list unpublished messages, etc.).
- `io.openleap.starter.core.messaging.service.MetricsService` – aggregates outbox/queue metrics (configurable).

Selected properties:

```yaml
ol:
  service:
    messaging:
      outbox:
        dispatcher:
          enabled: true
          fixedDelay: 1000
        maxAttempts: 10
        confirmTimeoutMillis: 5000
```

---

## Repository/JPA & auditing & multi‑tenancy

- `io.openleap.starter.core.repository.entity.OlPersistenceEntity` – base class for entities (e.g., IDs, auditing fields).
- `io.openleap.starter.core.repository.config.AuditingProviderConfig` – provides `AuditorAware`, etc.
- `io.openleap.starter.core.repository.config.JpaAuditingConfig` – enables JPA auditing.
- `io.openleap.starter.core.repository.config.TenantRlsAspect` – AOP/helpers for tenant evaluation/RLS.

Usage: derive your entities from `OlPersistenceEntity`, enable auditing (starter provides defaults), set tenant via `IdentityHolder`.

---

## Idempotency

- `io.openleap.starter.core.idempotency.IdempotencyRecordEntity` – stores idempotent operation keys.
- `io.openleap.starter.core.idempotency.IdempotencyRecordRepository` – access/query idempotency records.
- `io.openleap.starter.core.idempotency.IdempotencyRecordService` – service logic to check/register keys.
- `io.openleap.starter.core.idempotency.IdempotentReplayException` – thrown on replay.

Example (simplified):

```java
@Service
class PaymentService {
  private final IdempotencyRecordService idem;
  PaymentService(IdempotencyRecordService idem) { this.idem = idem; }

  @Transactional
  public void handle(String operationKey, Runnable action) {
    idem.runOnce(operationKey, action);
  }
}
```

---

## Error/response model (API)

- `io.openleap.starter.core.api.error.ErrorCode` – catalog of standard error codes with recommended HTTP status/default messages.
- `io.openleap.starter.core.api.error.ErrorResponse` – standardized error response for HTTP and other interfaces.

Example (explicit error with catalog code):

```java
class Example {
  void demo() {
    throw new org.springframework.web.server.ResponseStatusException(
      org.springframework.http.HttpStatus.BAD_REQUEST,
      io.openleap.starter.core.api.error.ErrorCode.BAD_REQUEST.name() + ": Invalid input"
    );
  }
}
```

---

## Utilities

- `io.openleap.starter.core.util.MoneyUtil` – helpers for monetary amounts/rounding, etc.

---

## File‑by‑file overview and usage (per package)

This section groups the most important files by package and explains when and how to use them. It complements the domain sections above and gives you concrete entry points.

### Package: `io.openleap.starter.core.config`

- `CoreServiceConfig`
  - What it is: Primary auto‑configuration entry that registers core beans and enables configuration properties.
  - How to use: Add the starter dependency; Spring Boot will pick it up automatically. No direct usage necessary.

- `OlStarterServiceProperties`
  - What it is: Strongly‑typed configuration under prefix `ol.service` (security mode, messaging exchanges, coverage, outbox, metrics, registry).
  - How to use: Configure in `application.yml`. Inject into your beans when you need to branch behavior.
  - Example:
    ```java
    @Service
    class FeatureToggle {
      private final OlStarterServiceProperties props;
      FeatureToggle(OlStarterServiceProperties props) { this.props = props; }
      boolean coverageEnabled() { return Boolean.TRUE.equals(props.getMessaging().getCoverage()); }
    }
    ```

- `JacksonConfig`
  - What it is: Shared Jackson customization (modules, naming, dates) used by HTTP and AMQP serialization.
  - How to use: Provided automatically; use normal `ObjectMapper`/`HttpMessageConverters`.

- `OtelConfig`
  - What it is: OpenTelemetry integration defaults (e.g., propagators/instrumentation helpers where applicable).
  - How to use: No action needed, follows Spring Boot conventions.

- `TraceIdFilter`
  - What it is: Servlet filter that ensures each HTTP request has a `traceId` in MDC for logs and propagation.
  - How to use: Automatic. Access via MDC in your logs.

- `IdentityHttpFilter`
  - What it is: Extracts request identity. In `iamsec` mode it uses JWT; in `nosec` mode it reads simple headers. Stores result in `IdentityHolder`.
  - How to use: Automatic. Read identity from `IdentityHolder` anywhere in request scope.

- `GlobalExceptionHandler`
  - What it is: Centralized HTTP error mapping that returns standardized `ErrorResponse` with `ErrorCode`.
  - How to use: Throw typical Spring exceptions or `ResponseStatusException` and let the handler format the response.

- `IdentityHolder`
  - What it is: Thread‑local identity context (`tenantId`, `userId`, `roles`, `scopes`). Used by HTTP and AMQP flows.
  - How to use: Read within business code; do not store between threads.
  - Example:
    ```java
    Long tenantId = IdentityHolder.getTenantId();
    ```

### Package: `io.openleap.starter.core.security`

- `SecurityKeycloakConfig`
  - What it is: Default Spring Security resource server configuration for OAuth2/JWT (Keycloak‑friendly).
  - How to use: Provide issuer/JWK settings as usual; Identity extraction is integrated.

- `SecurityLoggerConfig`
  - What it is: Adds logging helpers for security‑related events.
  - How to use: No direct usage; logs appear under appropriate categories.

- `JwtUtils`
  - What it is: Helpers to access common JWT claims (subject, tenant, roles, scopes).
  - How to use: Useful in custom security code if you need to read extra claims.

### Package: `io.openleap.starter.core.messaging`

- `RoutingKey`
  - What it is: Type to build/represent AMQP routing keys safely.
  - How to use: Create via `RoutingKey.of("domain.action")` when publishing.

- `MessageCoverageTracker` / `MessageCoverageReport`
  - What it is: Runtime tracker to assert/measure which messages were expected vs actually sent.
  - How to use: Register expectations at test or startup time, then query the report.

### Package: `io.openleap.starter.core.messaging.config`

- `MessagingConfig`
  - What it is: Auto‑configuration for RabbitMQ: exchanges, `RabbitTemplate`, message converter, and a preconfigured `starterRabbitListenerContainerFactory`.
  - How to use: Inject `RabbitTemplate` for low‑level operations or prefer `EventPublisher`. Use the provided listener container factory in `@RabbitListener`.
  - Example:
    ```java
    @RabbitListener(queues = "orders.queue", containerFactory = "starterRabbitListenerContainerFactory")
    void on(OrderCreated event) { /* ... */ }
    ```

- `MessagingIdentityPostProcessor`
  - What it is: Listener post‑processor that reconstructs `IdentityHolder` from AMQP headers.
  - How to use: Comes wired into the provided container factory; no action needed.

- `MessagingIdentityClearingAdvice`
  - What it is: Advice that clears identity after each message to avoid context leakage between messages.
  - How to use: Automatic via the container factory.

### Package: `io.openleap.starter.core.event`

- `OlDomainEvent`
  - What it is: Interface for domain events following the Thin Event (Notification) pattern.
  - How to use: Implement in your domain layer and use with `EventPublisher`.

### Package: `io.openleap.starter.core.messaging.event`

- `EventPayload`
  - What it is: Marker/base interface for event payload types to be serialized over AMQP.
  - How to use: Implement on your event DTOs to publish/consume with the starter’s converter.

- `EventPublisher`
  - What it is: High‑level publisher that writes events to the Outbox within the current transaction and triggers dispatch.
  - How to use: Inject and call `enqueue(routingKey, payload, headers)` inside transactional services.
  - Example:
    ```java
    publisher.enqueue(RoutingKey.of("customer.created"), new CustomerCreated(id), Map.of());
    ```

### Package: `io.openleap.starter.core.messaging.command`

- `Command`, `CommandHandler`, `CommandGateway`, `SimpleCommandBus`
  - What it is: Minimal command bus abstraction for synchronous in‑process command handling.
  - How to use: Define commands, register handlers, send via `CommandGateway`.
  - Example:
    ```java
    record CreateOrder(String id) implements Command {}
    @Component class CreateOrderHandler implements CommandHandler<CreateOrder> {
      public void handle(CreateOrder cmd) { /* do work */ }
    }
    ```

### Package: `io.openleap.starter.core.persistence.specification`

- `OlSpecificationBuilder`
  - What it is: Fluent builder for Spring Data JPA `Specification` objects.
  - How to use: Chain filters like `equal`, `like`, `in`, then call `build()`.
  - Example:
    ```java
    Specification<Order> spec = OlSpecificationBuilder.<Order>create()
        .equal("status", Status.PENDING)
        .like("customerName", "Doe")
        .build();
    ```

### Package: `io.openleap.starter.core.repository.entity`

- `OutboxEvent`
  - What it is: JPA entity representing an event waiting to be dispatched to AMQP (exchange, routing key, payload, headers, attempts).
  - How to use: Managed by the starter. You usually don’t touch it directly.

- `IdempotencyRecordEntity`
  - What it is: JPA entity storing idempotency keys of already processed operations.
  - How to use: Access via `IdempotencyRecordService`.

- `OlPersistenceEntity`
  - What it is: Base class providing common persistence/auditing fields.
  - How to use: Extend from it in your own JPA entities to inherit audit fields.

### Package: `io.openleap.starter.core.repository`

- `OutboxRepository`
  - What it is: Spring Data repository for `OutboxEvent` queries used by the dispatcher.
  - How to use: Usually internal; you can inject it for diagnostics if needed.

- `IdempotencyRecordRepository`
  - What it is: Repository for idempotency records.
  - How to use: Used by `IdempotencyRecordService`.

### Package: `io.openleap.starter.core.messaging.service`

- `OutboxDispatcher`
  - What it is: Background component that reads pending `OutboxEvent`s and sends them with publisher confirms and retry/backoff.
  - How to use: Enabled via properties; runs on a fixed delay. Wakes up after commit when publishing.

- `OutboxAdminService`
  - What it is: Convenience service to inspect or manage outbox state.
  - How to use: Inject for admin endpoints or support tooling.

- `MetricsService`
  - What it is: Aggregates metrics (e.g., queue length names configured in properties) for observability.
  - How to use: Inject and expose via actuator/custom endpoints.

### Package: `io.openleap.starter.core.repository.config`

- `AuditingProviderConfig`, `JpaAuditingConfig`
  - What it is: Enable and supply Spring Data JPA auditing (createdBy/createdAt, etc.).
  - How to use: Comes enabled; ensure your entities extend the base or carry auditing annotations.

- `TenantRlsAspect`
  - What it is: Aspect/utilities for tenant evaluation and row‑level security enforcement hooks.
  - How to use: Tenant is derived from `IdentityHolder`; ensure it’s set via HTTP/AMQP flows.

### Package: `io.openleap.starter.core.api`

- `ErrorCode`
  - What it is: Catalog of error codes used across responses and logs.
  - How to use: Reference when throwing exceptions or building error responses.

- `ErrorResponse`
  - What it is: Standardized error payload returned by `GlobalExceptionHandler`.
  - How to use: Returned automatically for typical Spring exceptions.

### Package: `io.openleap.starter.core.api.dto`

- `OlPageableResponseDto`
  - What it is: Generic record for paginated collection responses.
  - How to use: Wrap your response content and pagination metadata.

### Package: `io.openleap.starter.core.idempotency`

- `IdempotencyRecordService`
  - What it is: API to guard actions with an idempotency key.
  - How to use: Wrap side‑effecting operations with `runOnce(key, action)` to prevent duplicates.
  - Example:
    ```java
    idem.runOnce("payment-" + requestId, () -> processPayment(cmd));
    ```

- `IdempotentReplayException`
  - What it is: Thrown when an idempotent operation is attempted again.
  - How to use: Catch to implement graceful no‑ops or to surface 409/422 errors.

- `MoneyUtil`
  - What it is: Monetary helpers (rounding/formatting) used across services.
  - How to use: Static methods; safe for general use.

Notes
- Classes are auto‑discovered via Spring Boot autoconfiguration; you usually don’t need explicit `@Import`.
- Identity flows rely on `ol.service.security.mode` being correctly set for your environment.

---

## Integration checklist

1) Add the dependency (see quick start).
2) Configure `application.yml` as shown under “Auto‑configuration & properties”.
3) For AMQP listeners, use the `starterRabbitListenerContainerFactory` to enable identity processing.
4) Publish events via `EventPublisher` (Outbox pattern).
5) When running in IAM mode, ensure JWTs are available (HTTP: Bearer token or `X-JWT`; AMQP: `x-jwt`).

---

As of: 2026-01-03
