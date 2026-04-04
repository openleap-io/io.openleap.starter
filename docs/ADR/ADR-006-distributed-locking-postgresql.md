# ADR-006: Distributed Locking via PostgreSQL Advisory Locks

**Status:** Accepted

## Context

In a horizontally scaled deployment, multiple service instances may attempt to execute the same scheduled task or process the same resource concurrently. Traditional in-process locks (`synchronized`, `ReentrantLock`) do not work across JVM boundaries. An external coordination mechanism is needed that does not require additional infrastructure beyond the existing PostgreSQL database.

## Decision

The starter provides distributed locking via PostgreSQL session-level advisory locks. Locks are acquired and released through an AOP aspect triggered by the `@DistributedLock` annotation. Lock keys can be static strings or SpEL expressions resolved from method parameters.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `@DistributedLock` | `io.openleap.common.lock.aspect.DistributedLock` | Annotation to mark methods requiring a distributed lock |
| `DistributedLockAspect` | `io.openleap.common.lock.aspect.DistributedLockAspect` | AOP aspect that acquires/releases locks around annotated methods |
| `SessionAdvisoryLock` | `io.openleap.common.lock.SessionAdvisoryLock` | PostgreSQL advisory lock implementation |
| `DistributedLockConfig` | `io.openleap.common.lock.config.DistributedLockConfig` | Lock infrastructure configuration |
| `LockRepository` | `io.openleap.common.lock.db.LockRepository` | Lock repository interface |
| `PostgresLockRepository` | `io.openleap.common.lock.db.PostgresLockRepository` | PostgreSQL implementation of lock acquisition/release |
| `ConcurrentExecutionException` | `io.openleap.common.lock.exception.ConcurrentExecutionException` | Thrown when lock cannot be acquired and `failOnConcurrentExecution=true` |
| `LockException` | `io.openleap.common.lock.exception.LockException` | Base lock exception |
| `LockError` | `io.openleap.common.lock.exception.LockError` | Lock error enumeration |

### Annotation Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `key` | `String` | `""` | Static lock key |
| `keyExpression` | `String` | `""` | SpEL expression for dynamic lock key |
| `failOnConcurrentExecution` | `boolean` | `false` | If `true`, throws `ConcurrentExecutionException` when lock not acquired. If `false`, silently skips execution. |

### How It Works

```
1. Method annotated with @DistributedLock is called
2. DistributedLockAspect resolves the lock key (static or SpEL)
3. SessionAdvisoryLock attempts pg_try_advisory_lock(hash)
4a. Lock acquired → method executes → lock released via pg_advisory_unlock
4b. Lock not acquired + failOnConcurrentExecution=true → ConcurrentExecutionException
4c. Lock not acquired + failOnConcurrentExecution=false → method skipped silently
```

## Usage

### Static Lock Key

```java
@Service
class ScheduledTasks {

    @Scheduled(fixedDelay = 60_000)
    @DistributedLock(key = "outbox-cleanup")
    public void cleanupOutbox() {
        // Only one instance runs this at a time
        outboxAdminService.purgeOldEvents();
    }
}
```

### Dynamic Lock Key (SpEL)

```java
@Service
class PaymentService {

    @DistributedLock(
        keyExpression = "'payment-' + #orderId",
        failOnConcurrentExecution = true
    )
    public void processPayment(String orderId) {
        // Lock is per order; concurrent calls for same order throw ConcurrentExecutionException
    }
}
```

### Handling Lock Failures

```java
try {
    paymentService.processPayment(orderId);
} catch (ConcurrentExecutionException e) {
    // Another instance is processing this order
    log.info("Payment already being processed for order {}", orderId);
}
```

### Enabling Distributed Locking

```java
@SpringBootApplication
@EnableOpenLeapDistributedLocking
public class MyServiceApplication { }
```

## Configuration

No additional `application.yml` properties are required. The lock mechanism uses the existing PostgreSQL `DataSource`. Ensure `@EnableOpenLeapDistributedLocking` is present.

## Compliance Rules

1. `@EnableOpenLeapDistributedLocking` MUST be present on the application class.
2. Methods that must not run concurrently across instances MUST be annotated with `@DistributedLock`.
3. Either `key` or `keyExpression` MUST be provided — both cannot be empty.
4. Scheduled tasks that modify shared state MUST use `@DistributedLock` to prevent duplicate execution.
5. `failOnConcurrentExecution=true` MUST be used when callers need to know the lock was not acquired.
6. Lock keys MUST be deterministic — avoid random or time-based components.
7. The PostgreSQL database MUST be the same one used by the service (advisory locks are per-database).

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Using `synchronized` for cross-instance locking | Use `@DistributedLock` with PostgreSQL advisory locks |
| Using Redis/Zookeeper when PostgreSQL is already available | Advisory locks require no additional infrastructure |
| Long-running methods under `@DistributedLock` | Keep locked sections short; move heavy work outside the lock |
| Non-deterministic lock keys | Use stable identifiers (order ID, tenant ID, etc.) |
| Ignoring `ConcurrentExecutionException` | Handle gracefully — retry or inform the caller |
| Nesting `@DistributedLock` annotations | Avoid nested locks to prevent deadlocks |
