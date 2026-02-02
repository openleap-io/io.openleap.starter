# OpenLeap Starter

OpenLeap Starter is a core library for building microservices with Spring Boot. It provides a set of common features, pre-configured for consistency across the OpenLeap ecosystem.

## Project Structure (Separation of Concerns)

The project is structured by **feature**, following the principle of separation of concerns. Each feature is encapsulated within its own package under `io.openleap.common`.

Core features include:
- **HTTP / Security**: Authentication, identity management, and error handling for web requests.
- **Messaging**: RabbitMQ integration, event/command bus, and outbox pattern.
- **Persistence**: JPA base entities, auditing, and specification builders.
- **Lock**: Distributed locking mechanism using a database.
- **Idempotency**: Mechanism to handle duplicate requests.
- **Telemetry**: OpenTelemetry integration for tracing.
- **Utils**: Common utility classes (e.g., Jackson configuration, Money utilities).

---

## Configuration & Properties Pattern

Each feature in OpenLeap Starter typically follows a consistent configuration pattern:

1.  **Properties File (`*Properties.java`)**:
    *   Captures all configurable parameters for the respective feature.
    *   Annotated with `@ConfigurationProperties(prefix = "ol.<feature>")`.
    *   Provides type-safe access to application properties (e.g., from `application.yml`).
2.  **Config File (`*Config.java`)**:
    *   Bootstraps the feature's components (beans) into the Spring context.
    *   Annotated with `@Configuration` and often `@EnableConfigurationProperties(<Feature>Properties.class)`.
    *   Uses `@ConditionalOnProperty` or `@Profile` to enable/disable features based on configuration.

This approach ensures that each feature is self-contained and easily configurable.

---

## Features

### 1. HTTP & Security
Located in `io.openleap.common.http`.

*   **Security Mode**: Controlled by `ol.security.mode`.
    *   `nosec`: Plain identity info in headers (`X-Tenant-Id`, `X-User-Id`).
    *   `iamsec`: JWT-based security (Bearer token or `X-JWT` header).
*   **Identity**: `IdentityHolder` provides static access to the current user's identity (User ID, Tenant ID, Roles) throughout the request lifecycle.
*   **Error Handling**: Global exception handling with a standardized `ErrorResponse` and `ErrorCode`.

**Configuration**:
- `OpenleapSecurityConfig` / `OpenleapSecurityProperties` (prefix: `ol.security`)
- `SecurityKeycloakConfig` (active under `keycloak` profile)
- `SecurityLoggerConfig` (active under `nosec` or `logger` profiles)

### 2. Messaging (RabbitMQ)
Located in `io.openleap.common.messaging`.

Provides a robust messaging infrastructure using RabbitMQ, supporting both Events and Commands.

*   **Domain Events**: Base classes for creating and publishing domain events.
*   **Command Bus**: Simple implementation for handling commands asynchronously.
*   **Outbox Pattern**: Ensures "at-least-once" delivery by persisting messages in the database before publishing them to RabbitMQ.
*   **Identity Propagation**: Automatically carries user identity across messaging boundaries.

**Configuration**:
- `OpenleapMessagingConfig` / `OpenleapMessagingProperties` (prefix: `ol.messaging`)
- `MessagingConfig`: Core RabbitMQ setup (exchanges, converters, retry templates).
- `OutboxDispatcherConfig`: Configuration for the outbox message dispatcher.

### 3. Persistence
Located in `io.openleap.common.persistence`.

*   **Base Entities**: `PersistenceEntity` provides common fields like ID, version, and auditing timestamps.
*   **Auditing**: Automatic population of `@CreatedBy` and `@LastModifiedBy` using the current identity from `IdentityHolder`.
*   **Specifications**: `SpecificationBuilder` for dynamic JPA queries.

**Configuration**:
- `JpaAuditingConfig`: Enables Spring Data JPA auditing.
- `AuditingProviderConfig`: Provides the `AuditorAware` bean linked to `IdentityHolder`.

### 4. Distributed Lock
Located in `io.openleap.common.lock`.

Provides a `@DistributedLock` annotation to prevent concurrent execution of critical sections across multiple service instances.

**Configuration**:
- `DistributedLockConfig`: Configures the `LockRepository` and the aspect.

### 5. Idempotency
Located in `io.openleap.common.idempotency`.

Prevents duplicate processing of the same request by recording request IDs and their outcomes.

### 6. Telemetry
Located in `io.openleap.common.http.telemetry`.

Optional OpenTelemetry integration for distributed tracing.

**Configuration**:
- `OtelConfig`: Enabled via `ol.starter.tracing.otel.enabled=true`.

### 7. Utilities
Located in `io.openleap.common.util`.

*   **JacksonConfig**: Configures Jackson with `JavaTimeModule` and `MoneyModule`.
*   **MoneyUtil**: Utilities for handling monetary values consistently.

---

## Usage

To use this starter in your microservice, add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.openleap.starter</groupId>
    <artifactId>service</artifactId>
    <version>${openleap.starter.version}</version>
</dependency>
```

Then, configure the desired features in your `application.yml`. For example, to configure messaging:

```yaml
ol:
  messaging:
    events-exchange: "my-service.events"
    commands-exchange: "my-service.commands"
```
