# ADR-007: Idempotency & Duplicate Detection

**Status:** Accepted

## Context

Message consumers and API endpoints may receive the same request more than once due to retries, network issues, or at-least-once delivery guarantees. Without idempotency protection, duplicate processing can cause double charges, duplicate records, or inconsistent state. The system needs a mechanism to detect and safely handle duplicate operations.

## Decision

The starter provides an idempotency subsystem that records processed operation keys in a dedicated database table. Operations are checked against this table before execution. The `@Idempotent` annotation with AOP support enables declarative idempotency on any method.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `@Idempotent` | `io.openleap.common.idempotency.aspect.Idempotent` | Annotation marking a method as idempotent |
| `IdempotentAspect` | `io.openleap.common.idempotency.aspect.IdempotentAspect` | AOP aspect that checks/records idempotency keys |
| `IdempotencyRecordService` | `io.openleap.common.idempotency.IdempotencyRecordService` | Service for checking and registering idempotency keys |
| `IdempotencyRecordEntity` | `io.openleap.common.idempotency.IdempotencyRecordEntity` | JPA entity storing processed operation keys |
| `IdempotencyRecordRepository` | `io.openleap.common.idempotency.IdempotencyRecordRepository` | Repository for idempotency records |
| `DuplicateCommandException` | `io.openleap.common.idempotency.exception.DuplicateCommandException` | Thrown on duplicate when `failOnDuplicateExecution=true` |
| `IdempotentReplayException` | `io.openleap.common.idempotency.IdempotentReplayException` | Thrown on duplicate operation (programmatic usage) |

### `@Idempotent` Annotation Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `key` | `String` | `""` | Static idempotency key |
| `keyExpression` | `String` | `""` | SpEL expression for dynamic key resolution |
| `purpose` | `String` | `""` | Optional description stored with the record |
| `failOnDuplicateExecution` | `boolean` | `false` | If `true`, throws `DuplicateCommandException`. If `false`, silently returns `null`. |

### How It Works

```
1. Method annotated with @Idempotent is called
2. IdempotentAspect resolves the key (static or SpEL)
3. Check IdempotencyRecordService.alreadyProcessed(key)
4a. Not processed → execute method → markProcessed(key, purpose) → return result
4b. Already processed + failOnDuplicateExecution=true → throw DuplicateCommandException
4c. Already processed + failOnDuplicateExecution=false → return null (skip silently)
```

## Usage

### Declarative with `@Idempotent` (Recommended)

```java
@Service
class PaymentService {

    @Idempotent(
        keyExpression = "#command.paymentId()",
        purpose = "process-payment",
        failOnDuplicateExecution = true
    )
    @Transactional
    public PaymentResult processPayment(ProcessPaymentCommand command) {
        // This will only execute once per paymentId
        return doProcessPayment(command);
    }
}
```

### Declarative with Static Key

```java
@Service
class MigrationService {

    @Idempotent(key = "data-migration-v2", purpose = "one-time migration")
    public void runMigration() {
        // Runs exactly once across all instances
    }
}
```

### Programmatic Usage

```java
@Service
class OrderService {

    private final IdempotencyRecordService idempotencyService;

    @Transactional
    public void processOrder(String orderId, OrderCommand cmd) {
        if (idempotencyService.alreadyProcessed(orderId)) {
            throw new IdempotentReplayException("Order already processed: " + orderId);
        }

        Order order = doProcessOrder(cmd);

        idempotencyService.markProcessed(orderId, "process-order", order.getId().toString());
    }
}
```

### Enabling Idempotency

```java
@SpringBootApplication
@EnableOpenLeapIdempotency
public class MyServiceApplication { }
```

## Configuration

No additional `application.yml` properties are required. The idempotency table must be created via Flyway migration.

### Database Migration

| Script | Table | Purpose |
|--------|-------|---------|
| `V0.2__create_idempotency_table.sql` | `idempotency_record` | Stores processed operation keys with timestamps |

## Compliance Rules

1. `@EnableOpenLeapIdempotency` MUST be present on the application class.
2. Message consumers processing commands MUST be idempotent — use `@Idempotent` or `IdempotencyRecordService`.
3. The `idempotency_record` Flyway migration MUST be applied before the service starts.
4. Idempotency keys MUST be derived from business identifiers (e.g., payment ID, order ID) — not from technical IDs or timestamps.
5. Either `key` or `keyExpression` MUST be provided on `@Idempotent` — both cannot be empty.
6. `failOnDuplicateExecution=true` SHOULD be used for API endpoints where the caller needs feedback.
7. `failOnDuplicateExecution=false` SHOULD be used for message consumers where silent skip is acceptable.
8. Idempotency records SHOULD be periodically cleaned up to prevent unbounded table growth.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Using technical message IDs as idempotency keys | Use business identifiers (payment ID, order ID) |
| Checking idempotency outside the transaction | `@Idempotent` + `@Transactional` ensures atomicity |
| Not handling `DuplicateCommandException` in API layer | Return `409 Conflict` or appropriate HTTP status |
| Skipping idempotency for "safe" operations | All state-changing message handlers should be idempotent |
| Never cleaning up idempotency records | Schedule periodic cleanup of old records |
| Relying solely on database unique constraints | Use `@Idempotent` for explicit, auditable duplicate detection |
