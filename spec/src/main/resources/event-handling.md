# Event-Driven Messaging — Developer Guide (Starter)

This document defines the event-driven messaging model used by services depending on the `starter` module. It covers both outbound publishing (transactional outbox) and inbound processing (RabbitMQ listeners), and adds optional Avro serialization with Spring Cloud Schema Registry controlled by properties.

## What changes with this spec
- All writes and side-effecting operations in participating services SHOULD be triggered by events handled by RabbitMQ listeners (command/events-in). Domain code publishes events for other services via the outbox (events-out).
- Avro is the canonical schema format for events. When schema registry is enabled, producers and consumers will register/resolve schemas transparently.
- A property toggle controls registry usage: `ol.base.service.messaging.registry.enabled`.

## Components provided by `base`
- Outbound (no change in concept):
  - `io.openleap.starter.core.messaging.event.EventPublisher` — writes outbox records and triggers dispatch after commit.
  - `io.openleap.starter.core.messaging.service.OutboxDispatcher` — scheduled/triggered dispatcher to RabbitMQ with confirms & retries.
- Inbound (new):
  - Rabbit listener infrastructure (container factory and message converters) so services can implement `@RabbitListener` methods for their commands/events.
- Serialization:
  - JSON (default) via `Jackson2JsonMessageConverter` for backward compatibility.
  - Avro + Schema Registry (optional) via Spring Cloud Schema with `AvroSchemaMessageConverter` when enabled.

## Property model
```yaml
ol:
  base:
    service:
      messaging:
        events-exchange: openleap-events   # default topic exchange name
        coverage: false                    # existing feature
        registry:
          enabled: false                   # enable Spring Cloud Schema Registry
          url: http://localhost:8990       # registry server URL (when enabled)
          format: avro                     # schema format (only 'avro' supported for now)

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
```

Notes:
- When `registry.enabled=true`, the system uses Avro converters and integrates with the registry at `registry.url`.
- If disabled or misconfigured, the system falls back to JSON.

## Inbound processing (listeners)
- Services implement command/event handlers using Spring AMQP’s `@RabbitListener` on queues bound to the exchange/routing keys they care about.
- The base module provides a preconfigured `SimpleRabbitListenerContainerFactory` named `baseRabbitListenerContainerFactory` that selects Avro or JSON automatically according to the property above.
- Example:
```java
@RabbitListener(queues = "acc.core.command.create-account", containerFactory = "baseRabbitListenerContainerFactory")
public void handleCreateAccount(CreateAccountCommand command, @Headers Map<String, Object> headers) {
    // perform write side effects based on the command
}
```

## Outbound publishing (transactional outbox)
No behavioral changes; callers still use `EventPublisher.enqueue(exchange, routingKey, payload, headers)`. The dispatcher now serializes payloads using Avro+Schema when enabled, otherwise JSON.

## Avro schemas
- Place Avro schemas under `spec/src/main/resources/schemas` in `.avsc` files.
- Naming convention: `<domain>.<entity>.<event|command>.<action>-v<MAJOR>.<MINOR>.avsc`, e.g., `acc.account.command.create.v1.avsc` or `acc.account.event.created.v1.avsc`.
- Envelope fields recommended across schemas:
  - `eventId` (string, UUID), `traceId` (string), `occurredAt` (string, ISO-8601), `source` (string), plus domain-specific fields.
- When registry is enabled, producers register the schema, and consumers resolve it; message `contentType` will be `application/*+avro`.

### Example schema: generic envelope + sample domain event
File: `schemas/base.event-envelope.v1.avsc`
```json
{
  "type": "record",
  "name": "EventEnvelope",
  "namespace": "io.openleap.base.schema",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "traceId", "type": ["null", "string"], "default": null},
    {"name": "occurredAt", "type": "string"},
    {"name": "source", "type": "string"},
    {"name": "type", "type": "string"},
    {"name": "payload", "type": "string"}
  ]
}
```

File: `schemas/acc.account.event.created.v1.avsc`
```json
{
  "type": "record",
  "name": "AccountCreated",
  "namespace": "io.openleap.acc.schema",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "traceId", "type": ["null", "string"], "default": null},
    {"name": "occurredAt", "type": "string"},
    {"name": "accountId", "type": "string"},
    {"name": "tenantId", "type": "string"},
    {"name": "currency", "type": "string"}
  ]
}
```

## Exchange/queue binding
- Base defines a durable topic exchange `ol.base.service.messaging.events-exchange` (default `openleap-events`).
- Each service is responsible for creating its queues and bindings (via infra code or IaC) for the routing keys it needs.

## Migration guide
1. Enable listeners for write paths: define queues and implement `@RabbitListener` handlers executing writes.
2. Keep your existing outbox-based publishing for outbound events.
3. Opt into Avro+Schema Registry by setting `ol.base.service.messaging.registry.enabled=true` and supplying `url` and `format` (`avro`).
4. Add or migrate schemas under `spec/src/main/resources/schemas`.

## Testing
- Unit tests can keep using JSON payloads by default.
- Integration tests that enable the registry should run with a test registry (e.g., embedded or container) and verify Avro serialization/deserialization through the listener factory.
