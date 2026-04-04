# ADR-003: Transactional Outbox & RabbitMQ Messaging

**Status:** Accepted

## Context

Publishing messages to a broker (RabbitMQ) directly from a service risks inconsistency: if the database transaction commits but the broker publish fails, the event is lost. Conversely, if the broker publish succeeds but the transaction rolls back, a phantom event is sent. The transactional outbox pattern eliminates this dual-write problem by persisting events in the same database transaction as the business state change.

## Decision

The starter implements the transactional outbox pattern. `EventPublisher.enqueue()` inserts an `OutboxEvent` row in the same transaction as the domain write. A background `OutboxOrchestrator` polls for pending events and dispatches them via a configurable `OutboxDispatcher` (RabbitMQ or logger).

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `OutboxEvent` | `io.openleap.common.messaging.entity.OutboxEvent` | JPA entity representing a pending/dispatched event |
| `OutboxRepository` | `io.openleap.common.messaging.repository.OutboxRepository` | Repository for outbox queries |
| `EventPublisher` | `io.openleap.common.messaging.event.EventPublisher` | Enqueues events into the outbox within the current transaction |
| `OutboxOrchestrator` | `io.openleap.common.messaging.service.OutboxOrchestrator` | Coordinates outbox processing; wakes after commit |
| `OutboxProcessor` | `io.openleap.common.messaging.service.OutboxProcessor` | Reads pending events and dispatches via `OutboxDispatcher` |
| `OutboxAdminService` | `io.openleap.common.messaging.service.OutboxAdminService` | Admin operations (requeue, purge, inspect) |
| `MetricsService` | `io.openleap.common.messaging.service.MetricsService` | Outbox and queue metrics |
| `OutboxDispatcher` | `io.openleap.common.messaging.dispatcher.OutboxDispatcher` | Interface for dispatching events to external systems |
| `RabbitMqOutboxDispatcher` | `io.openleap.common.messaging.dispatcher.rabbitmq.RabbitMqOutboxDispatcher` | Production dispatcher using RabbitMQ with publisher confirms |
| `LoggingOutboxDispatcher` | `io.openleap.common.messaging.dispatcher.logger.LoggingOutboxDispatcher` | Logging stub for testing/development |
| `DispatchResult` | `io.openleap.common.messaging.dispatcher.DispatchResult` | Result DTO from a dispatch attempt |
| `OutboxDispatcherConfig` | `io.openleap.common.messaging.dispatcher.OutboxDispatcherConfig` | Dispatcher bean configuration |
| `MessagingConfig` | `io.openleap.common.messaging.config.MessagingConfig` | RabbitMQ infrastructure (exchange, template, converter) |
| `MessageTopologyConfiguration` | `io.openleap.common.messaging.config.MessageTopologyConfiguration` | Exchange and queue topology |
| `MessagingIdentityPostProcessor` | `io.openleap.common.messaging.config.MessagingIdentityPostProcessor` | Extracts identity from inbound AMQP headers |
| `MessagingIdentityClearingAdvice` | `io.openleap.common.messaging.config.MessagingIdentityClearingAdvice` | Clears `IdentityHolder` after message processing |
| `RetryableException` | `io.openleap.common.messaging.exception.RetryableException` | Signals retryable dispatch failure |
| `NonRetryableException` | `io.openleap.common.messaging.exception.NonRetryableException` | Signals non-retryable dispatch failure |

### Processing Flow

```
1. EventPublisher.enqueue()  →  INSERT into outbox_event (same transaction)
2. Transaction commits
3. OutboxOrchestrator wakes (if wakeup-after-commit=true) or scheduled poll
4. OutboxProcessor reads pending events
5. OutboxDispatcher dispatches to RabbitMQ (with publisher confirms)
6. On success: mark as published (or delete if delete-on-ack=true)
7. On failure: increment attempt count, retry with backoff up to max-attempts
```

### Dispatcher Types

| Type | Implementation | Use Case |
|------|---------------|----------|
| `rabbitmq` | `RabbitMqOutboxDispatcher` | Production — publishes to RabbitMQ with publisher confirms |
| `logger` | `LoggingOutboxDispatcher` | Testing/development — logs events to console |

## Usage

### Publishing Events (Always via Outbox)

```java
@Service
class OrderService {

    private final EventPublisher publisher;

    @Transactional
    public void createOrder(CreateOrderCommand cmd) {
        Order order = new Order(cmd);
        orderRepository.save(order);

        publisher.enqueue(
            RoutingKey.of("order.created"),
            new OrderCreatedEvent(order.getId().toString()),
            Map.of("source", "order-service")
        );
        // Event is written to outbox_event in the same transaction
    }
}
```

### Enabling Messaging

```java
@SpringBootApplication
@EnableOpenLeapMessaging
public class MyServiceApplication { }
```

## Configuration

```yaml
ol:
  messaging:
    enabled: true                                         # default: false
    events-exchange: ${OL_EVENTS_EXCHANGE:ol.exchange.events}  # default: ol.exchange.events
    outbox:
      dispatcher:
        enabled: true                                     # default: true
        type: rabbitmq                                    # rabbitmq | logger (default: rabbitmq)
        fixed-delay: 1000                                 # polling interval ms (default: 1000)
        wakeup-after-commit: true                         # default: true
        max-attempts: 10                                  # default: 10
        delete-on-ack: false                              # default: false
        confirm-timeout-millis: 5000                      # default: 5000
    retry:
      max-attempts: 3                                     # consumer retry (default: 3)
      initial-interval: 1000                              # ms (default: 1000)
      multiplier: 2.0                                     # default: 2.0
      max-interval: 10000                                 # ms (default: 10000)
```

### Database Migration

The starter provides a Flyway migration script:

| Script | Table | Purpose |
|--------|-------|---------|
| `V0.1__create_outbox_table.sql` | `outbox_event` | Stores pending and dispatched events |

## Compliance Rules

1. Events MUST be published via `EventPublisher.enqueue()` — never via `RabbitTemplate` directly.
2. `ol.messaging.enabled` MUST be set to `true` for messaging to function.
3. `@EnableOpenLeapMessaging` MUST be present on the application class.
4. The `outbox_event` Flyway migration MUST be applied before the service starts.
5. Production deployments MUST use `type: rabbitmq` dispatcher.
6. The `max-attempts` setting MUST be monitored — events exceeding max attempts require manual intervention via `OutboxAdminService`.
7. `delete-on-ack: true` SHOULD only be used when event replay is not required.
8. Consumer retry settings MUST be tuned per service based on downstream reliability.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Publishing to RabbitMQ directly via `RabbitTemplate` | Use `EventPublisher.enqueue()` for transactional safety |
| Publishing events outside `@Transactional` | Enqueue within the same transaction as the domain write |
| Setting `delete-on-ack: true` without replay strategy | Keep events for audit/replay; purge via `OutboxAdminService` |
| Ignoring failed dispatch attempts | Monitor `max-attempts` exhaustion; use `OutboxAdminService` for recovery |
| Using `logger` dispatcher in production | Switch to `rabbitmq` dispatcher for actual message delivery |
