# OpenLeap Core Starter

A modular Spring Boot starter providing common infrastructure for building microservices in the OpenLeap ecosystem. Each
module can be included independently — pick only what you need.

## Modules

| Module                               | Artifact           | Description                                                                                                          |
|--------------------------------------|--------------------|----------------------------------------------------------------------------------------------------------------------|
| [core-common](core-common)           | `core-common`      | Shared utilities and domain primitives (Check, UuidUtils, BusinessId, MoneyUtils, EncryptionService, IdentityHolder) |
| [core-web](core-web)                 | `core-web`         | REST API utilities, HTTP client interceptors, global error handling                                                  |
| [core-persistence](core-persistence) | `core-persistence` | JPA base entities, auditing, multi-tenant RLS, SpecificationBuilder                                                  |
| [core-security](core-security)       | `core-security`    | JWT/OAuth2 security, Keycloak integration, identity HTTP filter                                                      |
| [core-iam](core-iam)                 | `core-iam`         | IAM authorization client, `@RequiresPermission` AOP                                                                  |
| [core-messaging](core-messaging)     | `core-messaging`   | Transactional outbox pattern, RabbitMQ, command bus, domain event publishing                                         |
| [core-idempotency](core-idempotency) | `core-idempotency` | Idempotent command execution via `@Idempotent` AOP                                                                   |
| [core-lock](core-lock)               | `core-lock`        | PostgreSQL advisory lock-based distributed locking via `@DistributedLock`                                            |
| [core-telemetry](core-telemetry)     | `core-telemetry`   | OpenTelemetry configuration, trace ID propagation                                                                    |

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

---

## Quick Start

Add individual modules to your `pom.xml`:

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
<!-- ... -->
</dependencies>
```

Each module uses Spring Boot auto-configuration — just add the dependency and it activates automatically.

---

## Configuration

### Security (`ol.security`)

| Property | Default | Description                        |
|----------|---------|------------------------------------|
| `mode`   | `nosec` | Security mode: `nosec` or `iamsec` |

- **`nosec`**: Identity via headers (`X-Tenant-Id`, `X-User-Id`, `X-Scopes`, `X-Roles`)
- **`iamsec`**: JWT-based security (Bearer token or `X-JWT` header)

### IAM (`ol.iam`)

| Property         | Default                     | Description                        |
|------------------|-----------------------------|------------------------------------|
| `authz-base-url` | `http://iam-authz-svc:8082` | IAM authorization service base URL |

### Messaging (`ol.messaging`)

| Property                                   | Default              | Description                             |
|--------------------------------------------|----------------------|-----------------------------------------|
| `enabled`                                  | `false`              | Enable messaging feature                |
| `events-exchange`                          | `ol.exchange.events` | RabbitMQ exchange for events            |
| `coverage`                                 | `false`              | Enable message coverage tracking        |
| `outbox.dispatcher.enabled`                | `true`               | Enable outbox dispatcher                |
| `outbox.dispatcher.type`                   | `rabbitmq`           | Dispatcher type: `rabbitmq` or `logger` |
| `outbox.dispatcher.fixed-delay`            | `1000`               | Polling interval (ms)                   |
| `outbox.dispatcher.wakeup-after-commit`    | `true`               | Wake dispatcher after commit            |
| `outbox.dispatcher.max-attempts`           | `10`                 | Max dispatch attempts                   |
| `outbox.dispatcher.delete-on-ack`          | `false`              | Delete events after dispatch            |
| `outbox.dispatcher.confirm-timeout-millis` | `5000`               | Publisher confirm timeout               |
| `retry.max-attempts`                       | `3`                  | Message retry attempts                  |
| `retry.initial-interval`                   | `1000`               | Initial retry interval (ms)             |
| `retry.multiplier`                         | `2.0`                | Retry backoff multiplier                |
| `retry.max-interval`                       | `10000`              | Max retry interval (ms)                 |

### Telemetry (`ol.tracing.otel`)

| Property   | Default                 | Description            |
|------------|-------------------------|------------------------|
| `enabled`  | `false`                 | Enable OpenTelemetry   |
| `endpoint` | `http://localhost:4317` | OTLP exporter endpoint |

### Example `application.yml`

See [docs/application-example.yml](docs/application-example.yml) for a complete example with all available options.

```yaml
ol:
  security:
    mode: ${OL_SECURITY_MODE:nosec}
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

## Key Components

### Identity Management

```java
// Access identity anywhere in request lifecycle
UUID tenantId = IdentityHolder.getTenantId();
UUID userId = IdentityHolder.getUserId();
Set<String> roles = IdentityHolder.getRoles();

// Or inject via annotation
@GetMapping("/orders")
public List<Order> getOrders(@IdentityContext AuthenticatedIdentity identity) {
    return orderService.findByTenant(identity.tenantId());
}
```

### Event Publishing (Outbox Pattern)

```java

@Service
public class OrderService {
    private final EventPublisher publisher;

    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);
        publisher.enqueue(
                RoutingKey.of("order.created"),
                new OrderCreatedEvent(order.getId())
        );
    }
}
```

### Message Consumption

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

### Distributed Locking

```java

@DistributedLock(key = "payment-processing")
public void processPayments() {
    // Only one instance executes this at a time
}

@DistributedLock(keyExpression = "'order-' + #orderId", failOnConcurrentExecution = true)
public void processOrder(String orderId) {
    // Lock per order ID
}
```

### Idempotency

```java

@Idempotent(keyExpression = "#paymentId", purpose = "payment", failOnDuplicateExecution = true)
@Transactional
public void processPayment(String paymentId, PaymentCommand cmd) {
    Payment payment = doProcessPayment(cmd);
}
```

### IAM Authorization

```java

@RequiresPermission("orders:read")
@GetMapping("/orders")
public List<Order> getOrders() {
    // Access is checked against the IAM authorization service
}
```

### Persistence

```java
// Entity hierarchy: PersistenceEntity → AuditableEntity → VersionedEntity

// Dynamic queries with SpecificationBuilder
Specification<Order> spec = SpecificationBuilder.<Order>create()
        .equal("status", OrderStatus.PENDING)
        .like("customerName", searchTerm)
        .greaterThan("createdAt", startDate)
        .build();
```

---

## Database Migrations

Flyway scripts are provided in each module's `src/main/resources/db/migration/`:

| Script                               | Module           | Description              |
|--------------------------------------|------------------|--------------------------|
| `V0.1__create_outbox_table.sql`      | core-messaging   | Outbox event table       |
| `V0.2__create_idempotency_table.sql` | core-idempotency | Idempotency record table |

---

## Documentation

For detailed documentation, see [docs/core-service-starter.en.md](docs/core-service-starter.en.md).

---

## License

Dual-licensed under EUPL v1.2 and commercial license. See [license.txt](license.txt) for details.
