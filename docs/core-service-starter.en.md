# OpenLeap Core Starter – Features and Usage

This document describes all features provided by the OpenLeap Core Starter modules, structured by domain. It names fully
qualified classes, explains their purpose, and shows how to use them in microservices.

The project is organized as a multi-module Maven build. Each module can be included independently — pick only what you
need. All modules use Spring Boot auto-configuration, so features activate automatically when added to the classpath.

## Table of Contents

- [Overview & Quick Start](#overview--quick-start)
- [Configuration & Properties](#configuration--properties)
- [Security](#security)
- [IAM Authorization](#iam-authorization)
- [Web](#web)
- [Messaging (RabbitMQ)](#messaging-rabbitmq)
- [Outbox Pattern](#outbox-pattern)
- [Persistence & Auditing](#persistence--auditing)
- [Distributed Locking](#distributed-locking)
- [Idempotency](#idempotency)
- [Telemetry](#telemetry)
- [Common Utilities](#common-utilities)
- [Package Reference](#package-reference)
- [Integration Checklist](#integration-checklist)

---

## Overview & Quick Start

### Modules

| Module           | Artifact           | Root Package                   | Description                                                         |
|------------------|--------------------|--------------------------------|---------------------------------------------------------------------|
| core-common      | `core-common`      | `io.openleap.core.common`      | Shared utilities, domain primitives, IdentityHolder                 |
| core-web         | `core-web`         | `io.openleap.core.web`         | REST API utilities, HTTP client interceptors, error handling        |
| core-persistence | `core-persistence` | `io.openleap.core.persistence` | JPA base entities, auditing, multi-tenant RLS, SpecificationBuilder |
| core-security    | `core-security`    | `io.openleap.core.security`    | JWT/OAuth2 security, Keycloak integration, identity HTTP filter     |
| core-iam         | `core-iam`         | `io.openleap.core.iam`         | IAM authorization client, `@RequiresPermission` AOP                 |
| core-messaging   | `core-messaging`   | `io.openleap.core.messaging`   | Transactional outbox pattern, RabbitMQ, command bus, domain events  |
| core-idempotency | `core-idempotency` | `io.openleap.core.idempotency` | Idempotent command execution via `@Idempotent` AOP                  |
| core-lock        | `core-lock`        | `io.openleap.core.lock`        | PostgreSQL advisory lock-based distributed locking                  |
| core-telemetry   | `core-telemetry`   | `io.openleap.core.telemetry`   | OpenTelemetry configuration, trace ID propagation                   |

### Module Dependencies

```
core-common
├── core-web
├── core-persistence
│   └── core-idempotency
├── core-security
│   └── core-messaging (also depends on core-persistence, core-lock)
└── core-iam (also depends on core-web)

core-telemetry (standalone)
core-lock (standalone)
```

### Quick Start (Maven)

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.openleap.core</groupId>
            <artifactId>core-service-parent</artifactId>
            <version>4.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
<!-- Pick the modules you need -->
<dependency>
    <groupId>io.openleap.core</groupId>
    <artifactId>core-security</artifactId>
</dependency>
<dependency>
    <groupId>io.openleap.core</groupId>
    <artifactId>core-messaging</artifactId>
</dependency>
<dependency>
    <groupId>io.openleap.core</groupId>
    <artifactId>core-persistence</artifactId>
</dependency>
</dependencies>
```

Each module uses Spring Boot auto-configuration — just add the dependency and it activates automatically. No `@Enable*`
annotations are required.

### Auto-Configuration Classes

| Module           | Auto-Configuration             |
|------------------|--------------------------------|
| core-security    | `SecurityAutoConfiguration`    |
| core-web         | `WebAutoConfiguration`         |
| core-persistence | `PersistenceAutoConfiguration` |
| core-telemetry   | `TelemetryAutoConfiguration`   |
| core-messaging   | `MessagingAutoConfiguration`   |
| core-idempotency | `IdempotencyAutoConfiguration` |
| core-iam         | `IamAutoConfiguration`         |
| core-lock        | `LockAutoConfiguration`        |

---

## Configuration & Properties

### Property Prefixes

| Prefix            | Properties Class      | Module         | Description                     |
|-------------------|-----------------------|----------------|---------------------------------|
| `ol.messaging`    | `MessagingProperties` | core-messaging | Messaging configuration         |
| `ol.security`     | `SecurityProperties`  | core-security  | Security mode configuration     |
| `ol.iam`          | `IamProperties`       | core-iam       | IAM authorization configuration |
| `ol.tracing.otel` | (via `@Value`)        | core-telemetry | OpenTelemetry configuration     |

### MessagingProperties (`ol.messaging`)

Located in `io.openleap.core.messaging.config.MessagingProperties`

| Property                                   | Type    | Default                 | Description                              |
|--------------------------------------------|---------|-------------------------|------------------------------------------|
| `enabled`                                  | boolean | `false`                 | Enable messaging feature                 |
| `events-exchange`                          | String  | `ol.exchange.events`    | RabbitMQ exchange for events             |
| `coverage`                                 | boolean | `false`                 | Enable message coverage tracking         |
| `registry.enabled`                         | boolean | `false`                 | Enable schema registry                   |
| `registry.url`                             | String  | `http://localhost:8990` | Schema registry URL                      |
| `registry.format`                          | String  | `application/*+avro`    | Schema format                            |
| `outbox.dispatcher.enabled`                | boolean | `true`                  | Enable outbox dispatcher                 |
| `outbox.dispatcher.type`                   | String  | `rabbitmq`              | Dispatcher type: `rabbitmq` or `logger`  |
| `outbox.dispatcher.fixed-delay`            | long    | `1000`                  | Dispatcher polling interval (ms)         |
| `outbox.dispatcher.wakeup-after-commit`    | boolean | `true`                  | Wake dispatcher after transaction commit |
| `outbox.dispatcher.max-attempts`           | int     | `10`                    | Max dispatch attempts                    |
| `outbox.dispatcher.delete-on-ack`          | boolean | `false`                 | Delete events after successful dispatch  |
| `outbox.dispatcher.confirm-timeout-millis` | long    | `5000`                  | Publisher confirm timeout                |
| `retry.max-attempts`                       | int     | `3`                     | Message retry max attempts               |
| `retry.initial-interval`                   | long    | `1000`                  | Initial retry interval (ms)              |
| `retry.multiplier`                         | double  | `2.0`                   | Retry backoff multiplier                 |
| `retry.max-interval`                       | long    | `10000`                 | Max retry interval (ms)                  |
| `metrics.queues.main`                      | String  |                         | Main queue name for metrics              |
| `metrics.queues.dlq`                       | String  |                         | DLQ name for metrics                     |

### SecurityProperties (`ol.security`)

Located in `io.openleap.core.security.config.SecurityProperties`

| Property | Type | Default | Description                        |
|----------|------|---------|------------------------------------|
| `mode`   | enum | `nosec` | Security mode: `nosec` or `iamsec` |

### IamProperties (`ol.iam`)

Located in `io.openleap.core.iam.config.IamProperties`

| Property         | Type   | Default                     | Description                        |
|------------------|--------|-----------------------------|------------------------------------|
| `authz-base-url` | String | `http://iam-authz-svc:8082` | IAM authorization service base URL |

### Example `application.yml`

See [application-example.yml](application-example.yml) for a complete example with all available options.

```yaml
ol:
  security:
    mode: ${OL_SECURITY_MODE:nosec}
  iam:
    authz-base-url: ${OL_IAM_AUTHZ_BASE_URL:http://iam-authz-svc:8082}
  messaging:
    enabled: true
    events-exchange: ${OL_EVENTS_EXCHANGE:ol.exchange.events}
    outbox:
      dispatcher:
        enabled: true
        type: rabbitmq
    retry:
      max-attempts: 3
  tracing:
    otel:
      enabled: ${OL_OTEL_ENABLED:false}
      endpoint: ${OL_OTEL_ENDPOINT:http://localhost:4317}
```

---

## Security

Located in `io.openleap.core.security` (module: **core-security**)

### Security Modes

The starter supports two security modes controlled by `ol.security.mode`:

| Mode     | Description                                                                |
|----------|----------------------------------------------------------------------------|
| `nosec`  | Plain identity headers (`X-Tenant-Id`, `X-User-Id`, `X-Scopes`, `X-Roles`) |
| `iamsec` | JWT-based security (Bearer token or `X-JWT` header)                        |

### Key Classes

| Class                                  | Package                | Description                                        |
|----------------------------------------|------------------------|----------------------------------------------------|
| `SecurityAutoConfiguration`            | `...security.config`   | Auto-configuration entry point                     |
| `SecurityKeycloakConfig`               | `...security`          | Keycloak/OAuth2 resource server config             |
| `SecurityLoggerConfig`                 | `...security`          | Security logging helpers                           |
| `CustomJwtGrantedAuthoritiesConverter` | `...security`          | JWT to authorities converter                       |
| `NosecHeaderFilter`                    | `...security`          | Filter for nosec mode headers                      |
| `IdentityHttpFilter`                   | `...security.identity` | Extracts identity from HTTP requests               |
| `IdentityContext`                      | `...security.identity` | Annotation for injecting identity into controllers |
| `AuthenticatedIdentity`                | `...security.identity` | Record holding identity data                       |
| `IdentityContextArgumentResolver`      | `...security.identity` | Resolver for @IdentityContext                      |

### Usage Examples

**Reading identity in a controller:**

```java

@RestController
class ExampleController {

    @GetMapping("/me")
    public Map<String, Object> me() {
        return Map.of(
                "tenantId", String.valueOf(IdentityHolder.getTenantId()),
                "userId", String.valueOf(IdentityHolder.getUserId()),
                "roles", IdentityHolder.getRoles()
        );
    }
}
```

**Using `@IdentityContext` annotation:**

```java

@RestController
class OrderController {

    @GetMapping("/orders")
    public List<Order> getOrders(@IdentityContext AuthenticatedIdentity identity) {
        // identity.tenantId(), identity.userId(), identity.roles(), identity.scopes()
        return orderService.findByTenant(identity.tenantId());
    }
}
```

---

## IAM Authorization

Located in `io.openleap.core.iam` (module: **core-iam**)

### Key Classes

| Class                   | Package         | Description                                  |
|-------------------------|-----------------|----------------------------------------------|
| `IamAutoConfiguration`  | `...iam.config` | Auto-configuration entry point               |
| `AuthorizationService`  | `...iam`        | Service for checking permissions against IAM |
| `IamAuthzClient`        | `...iam.client` | REST client for IAM authorization service    |
| `@RequiresPermission`   | `...iam.aspect` | Annotation for declarative permission checks |
| `PermissionCheckAspect` | `...iam.aspect` | AOP aspect for `@RequiresPermission`         |

### Usage

```java

@RequiresPermission("orders:read")
@GetMapping("/orders")
public List<Order> getOrders() {
    // Access is checked against the IAM authorization service
}
```

---

## Web

Located in `io.openleap.core.web` (module: **core-web**)

### Key Classes

| Class                                | Package         | Description                           |
|--------------------------------------|-----------------|---------------------------------------|
| `WebAutoConfiguration`               | `...web.config` | Auto-configuration entry point        |
| `GlobalExceptionHandler`             | `...web.error`  | Centralized error handling            |
| `ErrorCode`                          | `...web.error`  | Standard error code catalog           |
| `ErrorResponse`                      | `...web.error`  | Standardized error response DTO       |
| `PageableResponseDto`                | `...web.api`    | Generic paginated response wrapper    |
| `ClientHttpRequestInterceptorConfig` | `...web.client` | HTTP client interceptor configuration |

---

## Messaging (RabbitMQ)

Located in `io.openleap.core.messaging` (module: **core-messaging**)

### Key Classes

| Class                             | Package               | Description                                               |
|-----------------------------------|-----------------------|-----------------------------------------------------------|
| `MessagingAutoConfiguration`      | `...messaging.config` | Auto-configuration entry point                            |
| `AmqpConfig`                      | `...messaging.config` | RabbitMQ configuration (exchanges, templates, converters) |
| `MessagingProperties`             | `...messaging.config` | Messaging configuration properties                        |
| `MessagingIdentityPostProcessor`  | `...messaging.config` | Extracts identity from AMQP headers                       |
| `MessagingIdentityClearingAdvice` | `...messaging.config` | Clears identity after message processing                  |
| `RoutingKey`                      | `...messaging`        | Type-safe routing key wrapper                             |
| `DomainEvent`                     | `...messaging.event`  | Interface for domain events                               |
| `BaseDomainEvent`                 | `...messaging.event`  | Base implementation of DomainEvent                        |
| `EventPublisher`                  | `...messaging.event`  | Transactional event publisher (writes to outbox)          |
| `MessageCoverageTracker`          | `...messaging`        | Tracks expected vs sent messages                          |

### Domain Events

The `DomainEvent` interface follows the Thin Event (Notification) pattern:

```java
public interface DomainEvent {
    String getAggregateId();

    String getAggregateType();

    String getChangeType();

    Instant getOccurredAt();

    Long getVersion();

    Map<String, Object> getMetadata();
}
```

### Publishing Events

```java

@Service
class OrderService {

    private final EventPublisher publisher;

    OrderService(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);

        publisher.enqueue(
                RoutingKey.of("order.created"),
                new OrderCreatedEvent(order.getId()),
                Map.of("source", "order-service")
        );
    }
}
```

### Consuming Messages

Use the `starterRabbitListenerContainerFactory` for automatic identity propagation:

```java

@RabbitListener(
        queues = "orders.queue",
        containerFactory = "starterRabbitListenerContainerFactory"
)
public void onOrderCreated(OrderCreatedEvent event) {
    // IdentityHolder is automatically populated from AMQP headers
    UUID tenantId = IdentityHolder.getTenantId();
}
```

### Command Bus

For synchronous in-process command handling:

| Class                               | Description                      |
|-------------------------------------|----------------------------------|
| `Command`                           | Marker interface for commands    |
| `CommandId`                         | Command identifier               |
| `CommandHandler<T extends Command>` | Handler interface                |
| `CommandGateway`                    | Gateway for dispatching commands |
| `SimpleCommandBus`                  | Default implementation           |

```java
// Define command
record CreateOrderCommand(String customerId, List<Item> items) implements Command {
}

// Define handler
@Component
class CreateOrderHandler implements CommandHandler<CreateOrderCommand> {
    public Object handle(CreateOrderCommand cmd) {
        return orderId;
    }
}

// Send command
@Service
class OrderFacade {
    private final CommandGateway gateway;

    public String createOrder(CreateOrderCommand cmd) {
        return gateway.send(cmd);
    }
}
```

---

## Outbox Pattern

Located in `io.openleap.core.messaging` (module: **core-messaging**)

The outbox pattern ensures reliable message delivery by persisting events in the database before dispatching to
RabbitMQ.

### Key Classes

| Class                      | Package                            | Description                             |
|----------------------------|------------------------------------|-----------------------------------------|
| `OutboxEvent`              | `...messaging.entity`              | JPA entity for outbox records           |
| `OutboxEventId`            | `...messaging.entity`              | Composite ID for OutboxEvent            |
| `OutboxRepository`         | `...messaging.repository`          | Repository for outbox queries           |
| `OutboxOrchestrator`       | `...messaging.service`             | Coordinates outbox processing           |
| `OutboxProcessor`          | `...messaging.service`             | Processes and dispatches pending events |
| `OutboxAdminService`       | `...messaging.service`             | Admin operations for outbox             |
| `MetricsService`           | `...messaging.service`             | Outbox/queue metrics                    |
| `OutboxDispatcher`         | `...messaging.dispatcher`          | Interface for message dispatch          |
| `RabbitMqOutboxDispatcher` | `...messaging.dispatcher.rabbitmq` | RabbitMQ implementation                 |
| `LoggingOutboxDispatcher`  | `...messaging.dispatcher.logger`   | Logging stub for testing                |

### Flow

1. `EventPublisher.enqueue()` writes event to `outbox_event` table within the current transaction
2. After commit, `OutboxOrchestrator` is notified (if `wakeup-after-commit=true`)
3. `OutboxProcessor` reads pending events and dispatches via `OutboxDispatcher`
4. On success, event is marked as published (or deleted if `delete-on-ack=true`)
5. On failure, retry with exponential backoff until `max-attempts` reached

### Dispatcher Types

Configure the dispatcher type using `ol.messaging.outbox.dispatcher.type`:

| Type       | Dispatcher                 | Description                          |
|------------|----------------------------|--------------------------------------|
| `rabbitmq` | `RabbitMqOutboxDispatcher` | Production dispatcher for RabbitMQ   |
| `logger`   | `LoggingOutboxDispatcher`  | Logging stub for testing/development |

---

## Persistence & Auditing

Located in `io.openleap.core.persistence` (module: **core-persistence**)

### Key Classes

| Class                          | Package                        | Description                                    |
|--------------------------------|--------------------------------|------------------------------------------------|
| `PersistenceAutoConfiguration` | `...persistence.config`        | Auto-configuration entry point                 |
| `PersistenceEntity`            | `...persistence.entity`        | Base entity with ID                            |
| `AuditableEntity`              | `...persistence.entity`        | Adds audit fields (createdAt, createdBy, etc.) |
| `VersionedEntity`              | `...persistence.entity`        | Adds optimistic locking version                |
| `SpecificationBuilder<T>`      | `...persistence.specification` | Fluent JPA Specification builder               |
| `AuditingProviderConfig`       | `...persistence.config`        | Provides AuditorAware linked to IdentityHolder |
| `TenantRlsAspect`              | `...persistence.config`        | Row-level security aspect                      |
| `@TenantAware`                 | `...persistence.tenant`        | Annotation for tenant-aware methods            |

### Entity Hierarchy

```
PersistenceEntity (id)
    └── AuditableEntity (createdAt, createdBy, lastModifiedAt, lastModifiedBy)
        └── VersionedEntity (version for optimistic locking)
```

### SpecificationBuilder

Build dynamic JPA queries fluently:

```java
Specification<Order> spec = SpecificationBuilder.<Order>create()
        .equal("status", OrderStatus.PENDING)
        .like("customerName", searchTerm)
        .greaterThan("createdAt", startDate)
        .in("region", List.of("EU", "US"))
        .build();

List<Order> orders = orderRepository.findAll(spec);
```

---

## Distributed Locking

Located in `io.openleap.core.lock` (module: **core-lock**)

Provides distributed locking using PostgreSQL advisory locks.

### Key Classes

| Class                          | Package             | Description                             |
|--------------------------------|---------------------|-----------------------------------------|
| `LockAutoConfiguration`        | `...lock.config`    | Auto-configuration entry point          |
| `@DistributedLock`             | `...lock.aspect`    | Annotation for locking methods          |
| `DistributedLockAspect`        | `...lock.aspect`    | AOP aspect for lock handling            |
| `SessionAdvisoryLock`          | `...lock`           | PostgreSQL advisory lock implementation |
| `LockRepository`               | `...lock.db`        | Lock repository interface               |
| `PostgresLockRepository`       | `...lock.db`        | PostgreSQL implementation               |
| `ConcurrentExecutionException` | `...lock.exception` | Thrown on lock failure                  |

### Usage

```java

@Service
class PaymentService {

    @DistributedLock(key = "payment-processing")
    public void processPayments() {
        // Only one instance can execute this at a time
    }

    @DistributedLock(
            keyExpression = "'order-' + #orderId",
            failOnConcurrentExecution = true
    )
    public void processOrder(String orderId) {
        // Lock per order ID; throws ConcurrentExecutionException if lock not acquired
    }
}
```

### Annotation Attributes

| Attribute                   | Type    | Default | Description                          |
|-----------------------------|---------|---------|--------------------------------------|
| `key`                       | String  | `""`    | Static lock key                      |
| `keyExpression`             | String  | `""`    | SpEL expression for dynamic key      |
| `failOnConcurrentExecution` | boolean | `false` | Throw exception if lock not acquired |

---

## Idempotency

Located in `io.openleap.core.idempotency` (module: **core-idempotency**)

Prevents duplicate processing of operations using idempotency keys. Supports both declarative (`@Idempotent`) and
programmatic usage.

### Key Classes

| Class                          | Package                 | Description                                       |
|--------------------------------|-------------------------|---------------------------------------------------|
| `IdempotencyAutoConfiguration` | `...idempotency.config` | Auto-configuration entry point                    |
| `@Idempotent`                  | `...idempotency.aspect` | Annotation for declarative idempotency            |
| `IdempotentAspect`             | `...idempotency.aspect` | AOP aspect for `@Idempotent`                      |
| `IdempotencyRecordEntity`      | `...idempotency`        | JPA entity storing processed operation keys       |
| `IdempotencyRecordRepository`  | `...idempotency`        | Repository for idempotency records                |
| `IdempotencyRecordService`     | `...idempotency`        | Service for checking/registering idempotency keys |

### `@Idempotent` Annotation Attributes

| Attribute                  | Type    | Default | Description                                                           |
|----------------------------|---------|---------|-----------------------------------------------------------------------|
| `key`                      | String  | `""`    | Static idempotency key                                                |
| `keyExpression`            | String  | `""`    | SpEL expression for dynamic key                                       |
| `purpose`                  | String  | `""`    | Optional purpose/description stored with the record                   |
| `failOnDuplicateExecution` | boolean | `false` | If true, throws `DuplicateCommandException`; if false, silently skips |

### Declarative Usage

```java

@Service
class PaymentService {

    @Idempotent(keyExpression = "#paymentId", purpose = "payment", failOnDuplicateExecution = true)
    @Transactional
    public void processPayment(String paymentId, PaymentCommand cmd) {
        Payment payment = doProcessPayment(cmd);
    }
}
```

### Programmatic Usage

```java

@Service
class PaymentService {

    private final IdempotencyRecordService idempotencyService;

    @Transactional
    public void processPayment(String paymentId, PaymentCommand cmd) {
        if (idempotencyService.alreadyProcessed(paymentId)) {
            throw new IdempotentReplayException("Payment already processed: " + paymentId);
        }

        Payment payment = doProcessPayment(cmd);
        idempotencyService.markProcessed(paymentId, "payment", payment.getId());
    }
}
```

---

## Telemetry

Located in `io.openleap.core.telemetry` (module: **core-telemetry**)

### Key Classes

| Class                        | Package               | Description                    |
|------------------------------|-----------------------|--------------------------------|
| `TelemetryAutoConfiguration` | `...telemetry.config` | Auto-configuration entry point |
| `OtelConfig`                 | `...telemetry.config` | OpenTelemetry configuration    |
| `TraceIdFilter`              | `...telemetry`        | Sets/propagates traceId in MDC |

### Configuration

```yaml
ol:
  tracing:
    otel:
      enabled: true
      endpoint: http://localhost:4317
```

### Usage

TraceId is automatically added to MDC for logging:

```java

@RestController
class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        log.info("Fetching order"); // traceId automatically in MDC
        return orderService.findById(id);
    }
}
```

---

## Common Utilities

Located in `io.openleap.core.common` (module: **core-common**)

### Identity

| Class            | Package              | Description                              |
|------------------|----------------------|------------------------------------------|
| `IdentityHolder` | `...common.identity` | Thread-local holder for identity context |

### Utilities

Located in `io.openleap.core.common.util`

| Class               | Description                                    |
|---------------------|------------------------------------------------|
| `MoneyUtils`        | Monetary amount helpers (rounding, formatting) |
| `UuidUtils`         | UUID generation utilities                      |
| `Check`             | Precondition checking utilities                |
| `EncryptionService` | Encryption/decryption utilities                |
| `UncheckedIO`       | Unchecked IO operation wrappers                |

### Domain Primitives

Located in `io.openleap.core.common.domain`

| Class          | Description                           |
|----------------|---------------------------------------|
| `BusinessId`   | Type-safe business identifier wrapper |
| `DomainEntity` | Base interface for domain entities    |

---

## Package Reference

### `io.openleap.core.common` (core-common)

| Sub-package | Key Files                                                                                       | Description                   |
|-------------|-------------------------------------------------------------------------------------------------|-------------------------------|
| `identity`  | `IdentityHolder.java`                                                                           | Thread-local identity context |
| `domain`    | `BusinessId.java`, `DomainEntity.java`                                                          | Domain primitives             |
| `util`      | `Check.java`, `UuidUtils.java`, `MoneyUtils.java`, `EncryptionService.java`, `UncheckedIO.java` | Utilities                     |
| `exception` |                                                                                                 | Common exceptions             |

### `io.openleap.core.security` (core-security)

| Sub-package | Key Files                                                                                                                         | Description         |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------|---------------------|
| `config`    | `SecurityAutoConfiguration.java`, `SecurityProperties.java`                                                                       | Configuration       |
| `identity`  | `IdentityHttpFilter.java`, `IdentityContext.java`, `AuthenticatedIdentity.java`, `IdentityContextArgumentResolver.java`           | Identity extraction |
| _(root)_    | `SecurityKeycloakConfig.java`, `SecurityLoggerConfig.java`, `NosecHeaderFilter.java`, `CustomJwtGrantedAuthoritiesConverter.java` | Security components |

### `io.openleap.core.web` (core-web)

| Sub-package | Key Files                                                             | Description              |
|-------------|-----------------------------------------------------------------------|--------------------------|
| `config`    | `WebAutoConfiguration.java`                                           | Configuration            |
| `error`     | `GlobalExceptionHandler.java`, `ErrorCode.java`, `ErrorResponse.java` | Error handling           |
| `api`       | `PageableResponseDto.java`                                            | API utilities            |
| `client`    | `ClientHttpRequestInterceptorConfig.java`                             | HTTP client interceptors |

### `io.openleap.core.iam` (core-iam)

| Sub-package | Key Files                                               | Description           |
|-------------|---------------------------------------------------------|-----------------------|
| `config`    | `IamAutoConfiguration.java`                             | Configuration         |
| `aspect`    | `RequiresPermission.java`, `PermissionCheckAspect.java` | Permission AOP        |
| `client`    | `IamAuthzClient.java`                                   | IAM REST client       |
| _(root)_    | `AuthorizationService.java`                             | Authorization service |

### `io.openleap.core.messaging` (core-messaging)

| Sub-package        | Key Files                                                                                                               | Description     |
|--------------------|-------------------------------------------------------------------------------------------------------------------------|-----------------|
| `config`           | `MessagingAutoConfiguration.java`, `AmqpConfig.java`, `MessagingProperties.java`, `MessagingIdentityPostProcessor.java` | Configuration   |
| `config.registrar` | `MessagingEntityRegistrar.java`, `MessagingRepositoryRegistrar.java`                                                    | JPA registrars  |
| `event`            | `DomainEvent.java`, `BaseDomainEvent.java`, `EventPublisher.java`                                                       | Domain events   |
| `command`          | `Command.java`, `CommandId.java`, `CommandHandler.java`, `CommandGateway.java`, `SimpleCommandBus.java`                 | Command bus     |
| `service`          | `OutboxOrchestrator.java`, `OutboxProcessor.java`, `OutboxAdminService.java`, `MetricsService.java`                     | Outbox services |
| `dispatcher`       | `OutboxDispatcher.java`, `RabbitMqOutboxDispatcher.java`, `LoggingOutboxDispatcher.java`                                | Dispatchers     |
| `entity`           | `OutboxEvent.java`, `OutboxEventId.java`                                                                                | JPA entities    |
| `repository`       | `OutboxRepository.java`                                                                                                 | Repositories    |
| _(root)_           | `RoutingKey.java`, `MessageCoverageTracker.java`                                                                        | Core classes    |

### `io.openleap.core.persistence` (core-persistence)

| Sub-package     | Key Files                                                                                  | Description       |
|-----------------|--------------------------------------------------------------------------------------------|-------------------|
| `config`        | `PersistenceAutoConfiguration.java`, `AuditingProviderConfig.java`, `TenantRlsAspect.java` | Configuration     |
| `entity`        | `PersistenceEntity.java`, `AuditableEntity.java`, `VersionedEntity.java`                   | Base entities     |
| `specification` | `SpecificationBuilder.java`                                                                | Query builder     |
| `tenant`        | `TenantAware.java`                                                                         | Tenant annotation |

### `io.openleap.core.idempotency` (core-idempotency)

| Sub-package        | Key Files                                                                | Description     |
|--------------------|--------------------------------------------------------------------------|-----------------|
| `config`           | `IdempotencyAutoConfiguration.java`                                      | Configuration   |
| `config.registrar` | `IdempotencyEntityRegistrar.java`, `IdempotencyRepositoryRegistrar.java` | JPA registrars  |
| `aspect`           | `Idempotent.java`, `IdempotentAspect.java`                               | Idempotency AOP |
| _(root)_           | `IdempotencyRecordEntity.java`, `IdempotencyRecordService.java`          | Core classes    |

### `io.openleap.core.lock` (core-lock)

| Sub-package | Key Files                                            | Description                  |
|-------------|------------------------------------------------------|------------------------------|
| `config`    | `LockAutoConfiguration.java`                         | Configuration                |
| `aspect`    | `DistributedLock.java`, `DistributedLockAspect.java` | Lock AOP                     |
| `db`        | `LockRepository.java`, `PostgresLockRepository.java` | Database access              |
| `exception` | `ConcurrentExecutionException.java`                  | Exceptions                   |
| _(root)_    | `SessionAdvisoryLock.java`                           | Advisory lock implementation |

### `io.openleap.core.telemetry` (core-telemetry)

| Sub-package | Key Files                                            | Description          |
|-------------|------------------------------------------------------|----------------------|
| `config`    | `TelemetryAutoConfiguration.java`, `OtelConfig.java` | Configuration        |
| _(root)_    | `TraceIdFilter.java`                                 | Trace ID propagation |

---

## Integration Checklist

1. **Add dependencies** — pick the modules you need in your `pom.xml` (see [Quick Start](#quick-start-maven))

2. **Configure `application.yml`** — set required properties (see [Configuration](#configuration--properties))

3. **Features activate automatically** — no `@Enable*` annotations needed; Spring Boot auto-configuration handles
   everything

4. **For AMQP listeners**, use `starterRabbitListenerContainerFactory` to enable identity propagation

5. **Publish events** via `EventPublisher` (uses outbox pattern)

6. **For IAM mode** (`ol.security.mode=iamsec`), ensure JWTs are available:
    - HTTP: Bearer token in `Authorization` header or `X-JWT` header
    - AMQP: `x-jwt` message header

7. **Run database migrations** for outbox and idempotency tables (Flyway scripts provided in each module)

---

## Database Migrations

Flyway migration scripts are provided in each module's `src/main/resources/db/migration/`:

| Script                               | Module           | Description                        |
|--------------------------------------|------------------|------------------------------------|
| `V0.1__create_outbox_table.sql`      | core-messaging   | Creates `outbox_event` table       |
| `V0.2__create_idempotency_table.sql` | core-idempotency | Creates `idempotency_record` table |

---

*Last updated: 2026-03-24*
