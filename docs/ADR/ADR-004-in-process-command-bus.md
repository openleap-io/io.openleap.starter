# ADR-004: In-Process Command Bus

**Status:** Accepted

## Context

Services need a way to dispatch commands to handlers within the same process without tight coupling between the caller and the handler. A command bus decouples the "what" (command) from the "how" (handler), enabling cleaner separation of concerns and making it easier to add cross-cutting behavior (logging, validation, authorization) around command handling.

## Decision

The starter provides an in-process command bus based on the Command pattern. Commands are dispatched synchronously via a `CommandGateway`, which delegates to the `SimpleCommandBus`. The bus auto-discovers all `CommandHandler` beans at startup and routes commands by type.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `Command` | `io.openleap.common.messaging.command.Command` | Marker interface for command objects |
| `CommandId` | `io.openleap.common.messaging.command.CommandId` | Command identifier |
| `CommandHandler<T>` | `io.openleap.common.messaging.command.CommandHandler` | Interface for command handlers, typed by command |
| `CommandGateway` | `io.openleap.common.messaging.command.CommandGateway` | Gateway for dispatching commands |
| `SimpleCommandBus` | `io.openleap.common.messaging.command.SimpleCommandBus` | Default implementation — auto-discovers handlers, routes by type |

### How It Works

```
Controller / Facade
    │
    ▼
CommandGateway.send(command)
    │
    ▼
SimpleCommandBus
    │  (looks up handler by command type)
    ▼
CommandHandler<T>.handle(command)
    │
    ▼
returns result
```

## Usage

### Define a Command

```java
public record CreateOrderCommand(
    String customerId,
    List<Item> items
) implements Command {}
```

### Implement a Handler

```java
@Component
class CreateOrderHandler implements CommandHandler<CreateOrderCommand> {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    CreateOrderHandler(OrderRepository orderRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Object handle(CreateOrderCommand cmd) {
        Order order = new Order(cmd.customerId(), cmd.items());
        orderRepository.save(order);

        eventPublisher.enqueue(
            RoutingKey.of("order.created"),
            new OrderCreatedEvent(order.getId().toString()),
            Map.of()
        );

        return order.getId();
    }
}
```

### Dispatch via Gateway

```java
@RestController
class OrderController {

    private final CommandGateway commandGateway;

    OrderController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderCommand cmd = new CreateOrderCommand(
            request.customerId(), request.items()
        );
        String orderId = commandGateway.send(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }
}
```

## Configuration

The command bus is auto-configured when `@EnableOpenLeapMessaging` is present. No additional properties are required.

```java
@SpringBootApplication
@EnableOpenLeapMessaging
public class MyServiceApplication { }
```

## Compliance Rules

1. Command objects MUST implement the `Command` marker interface.
2. Each command type MUST have exactly one `CommandHandler<T>` registered as a Spring bean (`@Component`).
3. Handlers MUST be stateless — use constructor-injected dependencies only.
4. Handlers SHOULD declare their own `@Transactional` boundaries.
5. The `CommandGateway` MUST be used for dispatch — never instantiate handlers directly.
6. Commands SHOULD be immutable (records or final fields).

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Calling `CommandHandler.handle()` directly | Use `CommandGateway.send()` for proper routing |
| Registering multiple handlers for the same command type | One handler per command type |
| Putting business logic in the controller | Move logic into the `CommandHandler` |
| Making commands mutable | Use Java records or classes with final fields |
| Using the command bus for cross-service communication | The command bus is in-process only; use domain events for cross-service |
