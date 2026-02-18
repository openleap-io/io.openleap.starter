# OpenLeap Core-Service Starter

A Spring Boot starter library providing common infrastructure for building microservices in the OpenLeap ecosystem.

## Quick Start

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.openleap.common</groupId>
    <artifactId>core-service</artifactId>
    <version>3.0.1-SNAPSHOT</version>
</dependency>
```

---

## Features

| Feature          | Package                             | 
|------------------|-------------------------------------| 
| HTTP & Security  | `io.openleap.common.http`           |
| Error Handling   | `io.openleap.common.http.error`     | 
| Messaging        | `io.openleap.common.messaging`      | 
| Persistence      | `io.openleap.common.persistence`    | 
| Distributed Lock | `io.openleap.common.lock`           | 
| Idempotency      | `io.openleap.common.idempotency`    | 
| Telemetry        | `io.openleap.common.http.telemetry` | 

---

## Configuration

### Security (`ol.security`)

| Property | Default | Description |
|----------|---------|-------------|
| `mode` | `nosec` | Security mode: `nosec` or `iamsec` |

- **`nosec`**: Identity via headers (`X-Tenant-Id`, `X-User-Id`, `X-Scopes`, `X-Roles`)
- **`iamsec`**: JWT-based security (Bearer token or `X-JWT` header)

### Messaging (`ol.messaging`)

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `false` | Enable messaging feature (required) |
| `events-exchange` | `ol.exchange.events` | RabbitMQ exchange for events |
| `coverage` | `false` | Enable message coverage tracking |
| `outbox.dispatcher.enabled` | `true` | Enable outbox dispatcher |
| `outbox.dispatcher.type` | `rabbitmq` | Dispatcher type: `rabbitmq` or `logger` |
| `outbox.dispatcher.fixed-delay` | `1000` | Polling interval (ms) |
| `outbox.dispatcher.wakeup-after-commit` | `true` | Wake dispatcher after commit |
| `outbox.dispatcher.max-attempts` | `10` | Max dispatch attempts |
| `outbox.dispatcher.delete-on-ack` | `false` | Delete events after dispatch |
| `outbox.dispatcher.confirm-timeout-millis` | `5000` | Publisher confirm timeout |
| `retry.max-attempts` | `3` | Message retry attempts |
| `retry.initial-interval` | `1000` | Initial retry interval (ms) |
| `retry.multiplier` | `2.0` | Retry backoff multiplier |
| `retry.max-interval` | `10000` | Max retry interval (ms) |

### Telemetry (`ol.tracing.otel`)

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `false` | Enable OpenTelemetry |
| `endpoint` | `http://localhost:4317` | OTLP exporter endpoint |

### Example `application.yml`

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
        fixed-delay: 1000
        max-attempts: 10
    retry:
      max-attempts: 3
      initial-interval: 1000
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
@Transactional
public void processPayment(String paymentId, PaymentCommand cmd) {
    if (idempotencyService.alreadyProcessed(paymentId)) {
        throw new IdempotentReplayException("Already processed: " + paymentId);
    }
    Payment payment = doProcessPayment(cmd);
    idempotencyService.markProcessed(paymentId, "payment", payment.getId());
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

Flyway scripts are provided in `src/main/resources/db/migration/`:

| Script | Description |
|--------|-------------|
| `V0.1__create_outbox_table.sql` | Outbox event table |
| `V0.2__create_idempotency_table.sql` | Idempotency record table |

---

## Documentation

For detailed documentation, see [docs/core-service-starter.en.md](docs/core-service-starter.en.md).

---

## License

Dual-licensed under EUPL v1.2 and commercial license. See [license.txt](license.txt) for details.

