# ADR-002: Domain Events — Thin Event / Notification Pattern

**Status:** Accepted

## Context

Microservices need to communicate state changes across bounded contexts without tight coupling. Full event payloads create schema coupling between producer and consumer. The thin event (notification) pattern solves this by publishing lightweight notifications that inform consumers *what* changed, allowing them to query for details if needed.

## Decision

The starter provides a `DomainEvent` interface following the thin event pattern. Events carry aggregate identity and change type but not the full aggregate state. Events are published transactionally via the outbox pattern (see [ADR-003](ADR-003-transactional-outbox-messaging.md)).

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `DomainEvent` | `io.openleap.common.messaging.event.DomainEvent` | Interface defining the event contract |
| `BaseDomainEvent` | `io.openleap.common.messaging.event.BaseDomainEvent` | Base implementation with common fields |
| `EventPublisher` | `io.openleap.common.messaging.event.EventPublisher` | Transactional event publisher (writes to outbox) |
| `RoutingKey` | `io.openleap.common.messaging.RoutingKey` | Type-safe routing key wrapper |
| `MessageCoverageTracker` | `io.openleap.common.messaging.MessageCoverageTracker` | Tracks expected vs. actually sent messages |
| `MessageCoverageReport` | `io.openleap.common.messaging.MessageCoverageReport` | Coverage report DTO |

### DomainEvent Interface

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

## Usage

### Defining a Domain Event

```java
public class OrderCreatedEvent extends BaseDomainEvent {

    public OrderCreatedEvent(String orderId) {
        super(orderId, "Order", "CREATED", 1L, Map.of());
    }
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
            new OrderCreatedEvent(order.getId().toString()),
            Map.of("source", "order-service")
        );
    }
}
```

### Consuming Events

Use `starterRabbitListenerContainerFactory` for automatic identity propagation from AMQP headers:

```java
@RabbitListener(
    queues = "orders.queue",
    containerFactory = "starterRabbitListenerContainerFactory"
)
public void onOrderCreated(OrderCreatedEvent event) {
    UUID tenantId = IdentityHolder.getTenantId();
    // Fetch full aggregate state if needed
    Order order = orderService.findById(event.getAggregateId());
}
```

### Routing Keys

```java
RoutingKey key = RoutingKey.of("order.created");
RoutingKey key = RoutingKey.of("order", "created");
```

### Message Coverage Tracking

Enable coverage tracking to verify all expected events are published during integration tests:

```yaml
ol:
  messaging:
    coverage: true
```

## Configuration

```yaml
ol:
  messaging:
    enabled: true
    events-exchange: ${OL_EVENTS_EXCHANGE:ol.exchange.events}
    coverage: ${OL_MESSAGE_COVERAGE:false}
```

## Compliance Rules

1. Domain events MUST implement `DomainEvent` or extend `BaseDomainEvent`.
2. Events MUST be published via `EventPublisher.enqueue()` — never send directly to RabbitMQ.
3. Events MUST carry only aggregate identity — not the full aggregate state (thin event pattern).
4. Routing keys MUST use dot notation matching the pattern `<aggregate>.<change-type>` (e.g., `order.created`).
5. Consumers MUST use `starterRabbitListenerContainerFactory` to receive automatic identity propagation.
6. Events MUST be published within a `@Transactional` boundary to leverage the outbox pattern.
7. Coverage tracking SHOULD be enabled in integration test profiles.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Publishing events outside a transaction | Always use `@Transactional` with `EventPublisher.enqueue()` |
| Including full aggregate state in events | Use thin events with aggregate ID; consumers query for details |
| Using `RabbitTemplate` directly | Use `EventPublisher` which writes to the outbox table |
| Hardcoding routing keys as plain strings | Use `RoutingKey.of()` for type safety |
| Consuming without `starterRabbitListenerContainerFactory` | Identity headers will not be propagated to `IdentityHolder` |
