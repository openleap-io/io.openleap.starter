# OpenLeap Core-Service Starter – Features and Usage

This document describes all starter features provided by the `core-service` module, structured by domain. It names fully qualified classes, explains their purpose, and shows how to use them in microservices.

All examples assume your service depends on the starter module and uses Spring Boot.

## Table of Contents

- [Overview & Quick Start](#overview--quick-start)
- [Configuration & Properties](#configuration--properties)
- [HTTP & Security](#http--security)
- [Messaging (RabbitMQ)](#messaging-rabbitmq)
- [Outbox Pattern](#outbox-pattern)
- [Persistence & Auditing](#persistence--auditing)
- [Distributed Locking](#distributed-locking)
- [Idempotency](#idempotency)
- [Telemetry](#telemetry)
- [Utilities](#utilities)
- [Package Reference](#package-reference)
- [Integration Checklist](#integration-checklist)

---

## Overview & Quick Start

The Core-Service Starter provides a comprehensive set of features for building microservices:

### Core Packages

| Package | Description |
|---------|-------------|
| `io.openleap.common.http` | HTTP infrastructure: security, identity, error handling, telemetry |
| `io.openleap.common.messaging` | RabbitMQ messaging: events, commands, outbox pattern |
| `io.openleap.common.persistence` | JPA entities, auditing, specifications |
| `io.openleap.common.lock` | Distributed locking using PostgreSQL advisory locks |
| `io.openleap.common.idempotency` | Idempotency key management |
| `io.openleap.common.domain` | Domain primitives (BusinessId, DomainEntity) |
| `io.openleap.common.util` | Utility classes |

### Quick Start (Maven)

```xml
<dependency>
    <groupId>io.openleap.common</groupId>
    <artifactId>core-service</artifactId>
    <version>3.0.0-SNAPSHOT</version>
</dependency>
```

---

## Configuration & Properties

### Property Prefixes

| Prefix | Properties Class | Description |
|--------|------------------|-------------|
| `ol.messaging` | `MessagingProperties` | Messaging configuration |
| `ol.security` | `OpenleapSecurityProperties` | Security mode configuration |
| `ol.tracing.otel` | (via `@Value`) | OpenTelemetry configuration |

### MessagingProperties (`ol.messaging`)

Located in `io.openleap.common.messaging.config.MessagingProperties`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean | `false` | Enable messaging feature (required) |
| `events-exchange` | String | `ol.exchange.events` | RabbitMQ exchange for events |
| `coverage` | boolean | `false` | Enable message coverage tracking |
| `registry.enabled` | boolean | `false` | Enable schema registry |
| `registry.url` | String | `http://localhost:8990` | Schema registry URL |
| `registry.format` | String | `avro` | Schema format |
| `outbox.dispatcher.enabled` | boolean | `true` | Enable outbox dispatcher |
| `outbox.dispatcher.type` | String | `rabbitmq` | Dispatcher type: `rabbitmq` or `logger` |
| `outbox.dispatcher.fixed-delay` | long | `1000` | Dispatcher polling interval (ms) |
| `outbox.dispatcher.wakeup-after-commit` | boolean | `true` | Wake dispatcher after transaction commit |
| `outbox.dispatcher.max-attempts` | int | `10` | Max dispatch attempts |
| `outbox.dispatcher.delete-on-ack` | boolean | `false` | Delete events after successful dispatch |
| `outbox.dispatcher.confirm-timeout-millis` | long | `5000` | Publisher confirm timeout |
| `retry.max-attempts` | int | `3` | Message retry max attempts |
| `retry.initial-interval` | long | `1000` | Initial retry interval (ms) |
| `retry.multiplier` | double | `2.0` | Retry backoff multiplier |
| `retry.max-interval` | long | `10000` | Max retry interval (ms) |
| `metrics.queues.main` | String | | Main queue name for metrics |
| `metrics.queues.dlq` | String | | DLQ name for metrics |

### OpenleapSecurityProperties (`ol.security`)

Located in `io.openleap.common.http.security.config.SecurityProperties`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mode` | enum | `nosec` | Security mode: `nosec` or `iamsec` |

### Example `application.yml`

```yaml
ol:
  messaging:
    enabled: true
    events-exchange: ${OL_EVENTS_EXCHANGE:ol.exchange.events}
    coverage: ${OL_MESSAGE_COVERAGE:false}
    registry:
      enabled: false
    outbox:
      dispatcher:
        enabled: true
        type: rabbitmq
        fixed-delay: 1000
        wakeup-after-commit: true
        max-attempts: 10
        delete-on-ack: false
        confirm-timeout-millis: 5000
    retry:
      max-attempts: 3
      initial-interval: 1000
      multiplier: 2.0
      max-interval: 10000
  tracing:
    otel:
      enabled: ${OL_OTEL_ENABLED:false}
      endpoint: ${OL_OTEL_ENDPOINT:http://localhost:4317}
  security:
    mode: ${OL_SECURITY_MODE:nosec}
```

---

## HTTP & Security

Located in `io.openleap.common.http`

### Security Modes

The starter supports two security modes controlled by `ol.security.mode`:

| Mode | Description |
|------|-------------|
| `nosec` | Plain identity headers (`X-Tenant-Id`, `X-User-Id`, `X-Scopes`, `X-Roles`) |
| `iamsec` | JWT-based security (Bearer token or `X-JWT` header) |

### Key Classes

| Class | Package | Description |
|-------|---------|-------------|
| `IdentityHolder` | `...http.security.identity` | Thread-local holder for identity context |
| `IdentityHttpFilter` | `...http.security.identity` | Extracts identity from HTTP requests |
| `IdentityContext` | `...http.security.identity` | Annotation for injecting identity into controllers |
| `AuthenticatedIdentity` | `...http.security.identity` | Record holding identity data |
| `OpenleapSecurityConfig` | `...http.security.config` | Main security configuration |
| `OpenleapSecurityProperties` | `...http.security.config` | Security properties |
| `SecurityKeycloakConfig` | `...http.security` | Keycloak/OAuth2 resource server config |
| `SecurityLoggerConfig` | `...http.security` | Security logging helpers |
| `JwtUtils` | `...http.security` | JWT claim extraction utilities |
| `GlobalExceptionHandler` | `...http.error` | Centralized error handling |
| `ErrorCode` | `...http.error` | Standard error code catalog |
| `ErrorResponse` | `...http.error` | Standardized error response DTO |
| `PageableResponseDto` | `...http.api` | Generic paginated response wrapper |

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

## Messaging (RabbitMQ)

Located in `io.openleap.common.messaging`

### Key Classes

| Class | Package | Description |
|-------|---------|-------------|
| `MessagingConfig` | `...messaging.config` | RabbitMQ configuration (exchanges, templates, converters) |
| `MessagingProperties` | `...messaging.config` | Messaging configuration properties |
| `MessagingIdentityPostProcessor` | `...messaging.config` | Extracts identity from AMQP headers |
| `MessagingIdentityClearingAdvice` | `...messaging.config` | Clears identity after message processing |
| `RoutingKey` | `...messaging` | Type-safe routing key wrapper |
| `DomainEvent` | `...messaging.event` | Interface for domain events |
| `BaseDomainEvent` | `...messaging.event` | Base implementation of DomainEvent |
| `EventPublisher` | `...messaging.event` | Transactional event publisher (writes to outbox) |
| `MessageCoverageTracker` | `...messaging` | Tracks expected vs sent messages |
| `MessageCoverageReport` | `...messaging` | Coverage report DTO |

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
        // Business logic...
        orderRepository.save(order);

        // Publish event via outbox
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
    // Process event...
}
```

### Command Bus

For synchronous in-process command handling:

| Class | Description |
|-------|-------------|
| `Command` | Marker interface for commands |
| `CommandId` | Command identifier |
| `CommandHandler<T extends Command>` | Handler interface |
| `CommandGateway` | Gateway for dispatching commands |
| `SimpleCommandBus` | Default implementation |

```java
// Define command
record CreateOrderCommand(String customerId, List<Item> items) implements Command {}

// Define handler
@Component
class CreateOrderHandler implements CommandHandler<CreateOrderCommand> {
    public Object handle(CreateOrderCommand cmd) {
        // Create order...
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

Located in `io.openleap.common.messaging`

The outbox pattern ensures reliable message delivery by persisting events in the database before dispatching to RabbitMQ.

### Key Classes

| Class | Package | Description |
|-------|---------|-------------|
| `OutboxEvent` | `...messaging.entity` | JPA entity for outbox records |
| `OutboxEventId` | `...messaging.entity` | Composite ID for OutboxEvent |
| `OutboxRepository` | `...messaging.repository` | Repository for outbox queries |
| `OutboxOrchestrator` | `...messaging.service` | Coordinates outbox processing |
| `OutboxProcessor` | `...messaging.service` | Processes and dispatches pending events |
| `OutboxAdminService` | `...messaging.service` | Admin operations for outbox |
| `MetricsService` | `...messaging.service` | Outbox/queue metrics |
| `OutboxDispatcher` | `...messaging.dispatcher` | Interface for message dispatch |
| `RabbitMqOutboxDispatcher` | `...messaging.dispatcher.rabbitmq` | RabbitMQ implementation |
| `LoggingOutboxDispatcher` | `...messaging.dispatcher.logger` | Logging stub for testing |

### Flow

1. `EventPublisher.enqueue()` writes event to `outbox_event` table within the current transaction
2. After commit, `OutboxOrchestrator` is notified (if `wakeup-after-commit=true`)
3. `OutboxProcessor` reads pending events and dispatches via `OutboxDispatcher`
4. On success, event is marked as published (or deleted if `delete-on-ack=true`)
5. On failure, retry with exponential backoff until `max-attempts` reached

### Dispatcher Types

Configure the dispatcher type using `ol.messaging.outbox.dispatcher.type`:

| Type | Dispatcher | Description |
|------|------------|-------------|
| `rabbitmq` | `RabbitMqOutboxDispatcher` | Production dispatcher for RabbitMQ |
| `logger` | `LoggingOutboxDispatcher` | Logging stub for testing/development |

---

## Persistence & Auditing

Located in `io.openleap.common.persistence`

### Key Classes

| Class | Package | Description |
|-------|---------|-------------|
| `PersistenceEntity` | `...persistence.entity` | Base entity with ID |
| `AuditableEntity` | `...persistence.entity` | Adds audit fields (createdAt, createdBy, etc.) |
| `VersionedEntity` | `...persistence.entity` | Adds optimistic locking version |
| `SpecificationBuilder<T>` | `...persistence.specification` | Fluent JPA Specification builder |
| `JpaAuditingConfig` | `...persistence.config` | Enables JPA auditing |
| `AuditingProviderConfig` | `...persistence.config` | Provides AuditorAware linked to IdentityHolder |
| `TenantRlsAspect` | `...persistence.config` | Row-level security aspect |

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

Located in `io.openleap.common.lock`

Provides distributed locking using PostgreSQL advisory locks.

### Key Classes

| Class | Package | Description |
|-------|---------|-------------|
| `@DistributedLock` | `...lock.aspect` | Annotation for locking methods |
| `DistributedLockAspect` | `...lock.aspect` | AOP aspect for lock handling |
| `SessionAdvisoryLock` | `...lock` | PostgreSQL advisory lock implementation |
| `DistributedLockConfig` | `...lock.config` | Lock configuration |
| `LockRepository` | `...lock.db` | Lock repository interface |
| `PostgresLockRepository` | `...lock.db` | PostgreSQL implementation |
| `ConcurrentExecutionException` | `...lock.exception` | Thrown on lock failure |

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

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `key` | String | `""` | Static lock key |
| `keyExpression` | String | `""` | SpEL expression for dynamic key |
| `failOnConcurrentExecution` | boolean | `false` | Throw exception if lock not acquired |

---

## Idempotency

Located in `io.openleap.common.idempotency`

Prevents duplicate processing of operations using idempotency keys.

### Key Classes

| Class | Description |
|-------|-------------|
| `IdempotencyRecordEntity` | JPA entity storing processed operation keys |
| `IdempotencyRecordRepository` | Repository for idempotency records |
| `IdempotencyRecordService` | Service for checking/registering idempotency keys |
| `IdempotentReplayException` | Thrown on duplicate operation |

### Usage

```java
@Service
class PaymentService {

    private final IdempotencyRecordService idempotencyService;

    @Transactional
    public void processPayment(String paymentId, PaymentCommand cmd) {
        if (idempotencyService.alreadyProcessed(paymentId)) {
            throw new IdempotentReplayException("Payment already processed: " + paymentId);
        }

        // Process payment...
        Payment payment = doProcessPayment(cmd);

        // Mark as processed
        idempotencyService.markProcessed(paymentId, "payment", payment.getId());
    }
}
```

---

## Telemetry

Located in `io.openleap.common.http.telemetry`

### Key Classes

| Class | Description |
|-------|-------------|
| `OtelConfig` | OpenTelemetry configuration |
| `TraceIdFilter` | Sets/propagates traceId in MDC |

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

## Utilities

Located in `io.openleap.common.util`

| Class | Description |
|-------|-------------|
| `MoneyUtil` | Monetary amount helpers (rounding, formatting) |
| `OpenleapUuid` | UUID generation utilities |
| `Check` | Precondition checking utilities |
| `EncryptionService` | Encryption/decryption utilities |
| `UncheckedIO` | Unchecked IO operation wrappers |

### Domain Primitives

Located in `io.openleap.common.domain`

| Class | Description |
|-------|-------------|
| `BusinessId` | Type-safe business identifier wrapper |
| `DomainEntity` | Base interface for domain entities |

---

## Package Reference

### `io.openleap.common.http.security`

| File | Description |
|------|-------------|
| `EnableOpenLeapSecurity.java` | Enable annotation |
| `OpenleapSecurityConfig.java` | Main security configuration |
| `OpenleapSecurityProperties.java` | Security properties |
| `SecurityKeycloakConfig.java` | Keycloak OAuth2 configuration |
| `SecurityLoggerConfig.java` | Security logging |
| `JwtUtils.java` | JWT utilities |
| `NosecHeaderFilter.java` | Filter for nosec mode headers |
| `CustomJwtGrantedAuthoritiesConverter.java` | JWT to authorities converter |

### `io.openleap.common.http.security.identity`

| File | Description |
|------|-------------|
| `IdentityHolder.java` | Thread-local identity context |
| `IdentityHttpFilter.java` | HTTP identity extraction filter |
| `IdentityContext.java` | Controller parameter annotation |
| `IdentityContextArgumentResolver.java` | Resolver for @IdentityContext |
| `AuthenticatedIdentity.java` | Identity data record |
| `IdentityWebConfig.java` | Web MVC configuration |

### `io.openleap.common.http.error`

| File | Description |
|------|-------------|
| `EnableOpenLeapErrorHandling.java` | Enable annotation |
| `GlobalExceptionHandler.java` | Centralized exception handling |
| `ErrorCode.java` | Error code catalog |
| `ErrorResponse.java` | Error response DTO |

### `io.openleap.common.http.telemetry`

| File | Description |
|------|-------------|
| `EnableOpenLeapTelemetry.java` | Enable annotation |
| `OtelConfig.java` | OpenTelemetry configuration |
| `TraceIdFilter.java` | Trace ID filter |

### `io.openleap.common.messaging`

| File | Description |
|------|-------------|
| `EnableOpenLeapMessaging.java` | Enable annotation |
| `RoutingKey.java` | Routing key wrapper |
| `MessageCoverageTracker.java` | Coverage tracking |
| `MessageCoverageReport.java` | Coverage report |

### `io.openleap.common.messaging.config`

| File | Description |
|------|-------------|
| `MessagingConfig.java` | RabbitMQ configuration |
| `MessagingProperties.java` | Messaging properties |
| `MessagingIdentityPostProcessor.java` | AMQP identity extraction |
| `MessagingIdentityClearingAdvice.java` | Identity cleanup advice |
| `MessageTopologyConfiguration.java` | Exchange/queue topology |

### `io.openleap.common.messaging.event`

| File | Description |
|------|-------------|
| `DomainEvent.java` | Domain event interface |
| `BaseDomainEvent.java` | Base implementation |
| `EventPublisher.java` | Transactional publisher |

### `io.openleap.common.messaging.command`

| File | Description |
|------|-------------|
| `Command.java` | Command marker interface |
| `CommandId.java` | Command identifier |
| `CommandHandler.java` | Handler interface |
| `CommandGateway.java` | Gateway interface |
| `SimpleCommandBus.java` | Default implementation |

### `io.openleap.common.messaging.service`

| File | Description |
|------|-------------|
| `OutboxOrchestrator.java` | Outbox coordination |
| `OutboxProcessor.java` | Event processing |
| `OutboxAdminService.java` | Admin operations |
| `MetricsService.java` | Metrics aggregation |

### `io.openleap.common.messaging.dispatcher`

| File | Description |
|------|-------------|
| `OutboxDispatcher.java` | Dispatcher interface |
| `OutboxDispatcherConfig.java` | Dispatcher configuration |
| `DispatchResult.java` | Dispatch result DTO |
| `RabbitMqOutboxDispatcher.java` | RabbitMQ implementation |
| `LoggingOutboxDispatcher.java` | Logging stub |

### `io.openleap.common.messaging.entity`

| File | Description |
|------|-------------|
| `OutboxEvent.java` | Outbox JPA entity |
| `OutboxEventId.java` | Composite ID |

### `io.openleap.common.persistence`

| File | Description |
|------|-------------|
| `PersistenceEntity.java` | Base entity |
| `AuditableEntity.java` | Auditable entity |
| `VersionedEntity.java` | Versioned entity |
| `SpecificationBuilder.java` | Query specification builder |

### `io.openleap.common.persistence.config`

| File | Description |
|------|-------------|
| `EnableOpenLeapAuditingJpa.java` | Enable annotation |
| `JpaAuditingConfig.java` | JPA auditing config |
| `AuditingProviderConfig.java` | Auditor provider |
| `TenantRlsAspect.java` | Row-level security |

### `io.openleap.common.lock`

| File | Description |
|------|-------------|
| `EnableOpenLeapDistributedLocking.java` | Enable annotation |
| `DistributedLock.java` | Lock annotation |
| `DistributedLockAspect.java` | Lock aspect |
| `SessionAdvisoryLock.java` | PostgreSQL advisory lock |
| `DistributedLockConfig.java` | Lock configuration |

### `io.openleap.common.idempotency`

| File | Description |
|------|-------------|
| `EnableOpenLeapIdempotency.java` | Enable annotation |
| `IdempotencyRecordEntity.java` | JPA entity |
| `IdempotencyRecordRepository.java` | Repository |
| `IdempotencyRecordService.java` | Service |
| `IdempotentReplayException.java` | Replay exception |

---

## Integration Checklist

1. **Add dependency** to your `pom.xml`:
   ```xml
   <dependency>
       <groupId>io.openleap.common</groupId>
       <artifactId>core-service</artifactId>
       <version>3.0.0-SNAPSHOT</version>
   </dependency>
   ```

2. **Configure `application.yml`** with required properties (see Configuration section)

3. **Enable features** using annotations:
   ```java
   @SpringBootApplication
   public class MyServiceApplication {
       public static void main(String[] args) {
           SpringApplication.run(MyServiceApplication.class, args);
       }
   }
   ```

4. **For AMQP listeners**, use `starterRabbitListenerContainerFactory` to enable identity propagation

5. **Publish events** via `EventPublisher` (uses outbox pattern)

6. **For IAM mode** (`ol.security.mode=iamsec`), ensure JWTs are available:
   - HTTP: Bearer token in `Authorization` header or `X-JWT` header
   - AMQP: `x-jwt` message header

7. **Run database migrations** for outbox and idempotency tables (Flyway scripts provided)

---

## Database Migrations

The starter provides Flyway migration scripts in `src/main/resources/db/migration/`:

| Script | Description |
|--------|-------------|
| `V1__create_outbox_table.sql` | Creates `outbox_event` table |
| `V2__create_idempotency_table.sql` | Creates `idempotency_record` table |

---

*Last updated: 2026-02-10*

