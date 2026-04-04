# ADR-010: Domain Primitives & Utilities

**Status:** Accepted

## Context

Microservices share common concerns: type-safe business identifiers, precondition validation, monetary calculations, and encryption. Duplicating these utilities across services leads to inconsistency and bugs. A shared set of domain primitives and utilities ensures uniform behavior and reduces boilerplate.

## Decision

The starter provides domain primitive types and utility classes that all services should use for common operations.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `BusinessId` | `io.openleap.common.domain.BusinessId` | Type-safe wrapper for business identifiers |
| `DomainEntity` | `io.openleap.common.domain.DomainEntity` | Base interface for domain entities with a business identity |
| `Check` | `io.openleap.common.util.Check` | Precondition validation utilities |
| `MoneyUtils` | `io.openleap.common.util.MoneyUtils` | Monetary amount helpers (rounding, formatting) |
| `EncryptionService` | `io.openleap.common.util.EncryptionService` | Encryption and decryption utilities |
| `UncheckedIO` | `io.openleap.common.util.UncheckedIO` | Wraps checked IO exceptions into unchecked |
| `ReflectionUtils` | `io.openleap.common.ReflectionUtils` | Reflection helper utilities |

## Usage

### BusinessId — Type-Safe Identifiers

```java
@Embeddable
public class OrderId extends BusinessId {

    protected OrderId() {
        super();
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    private OrderId(String value) {
        super(value);
    }
}
```

Use in entities:

```java
@Entity
public class Order extends AuditableEntity implements DomainEntity<OrderId> {

    @Embedded
    private OrderId businessId;

    @Override
    public OrderId getBusinessId() {
        return businessId;
    }
}
```

### DomainEntity Interface

```java
public interface DomainEntity<ID extends BusinessId> {
    ID getBusinessId();
}
```

All domain entities should implement this interface to establish a business identity distinct from the persistence ID.

### Check — Precondition Validation

```java
@Service
class OrderService {

    public void createOrder(String customerId, List<Item> items) {
        Check.notNull(customerId, "customerId must not be null");
        Check.notEmpty(items, "items must not be empty");
        Check.isTrue(items.size() <= 100, "order cannot exceed 100 items");

        // Proceed with order creation
    }
}
```

### MoneyUtils — Monetary Calculations

```java
BigDecimal price = MoneyUtils.round(rawPrice);  // Rounds to 2 decimal places
String formatted = MoneyUtils.format(price);     // Formats for display
```

### EncryptionService

```java
@Service
class SensitiveDataService {

    private final EncryptionService encryptionService;

    public String encrypt(String plainText) {
        return encryptionService.encrypt(plainText);
    }

    public String decrypt(String cipherText) {
        return encryptionService.decrypt(cipherText);
    }
}
```

### UncheckedIO — Clean Exception Handling

```java
String content = UncheckedIO.run(() -> Files.readString(path));
// Throws UncheckedIOException instead of checked IOException
```

## Configuration

No additional `application.yml` properties are required. These classes are available as soon as the `core-service` dependency is on the classpath.

## Compliance Rules

1. Business identifiers MUST extend `BusinessId` — never use raw `String` or `UUID` for external-facing IDs.
2. Domain entities MUST implement `DomainEntity<T>` to expose their business identity.
3. Precondition checks MUST use `Check` — not manual `if/throw` patterns — for consistency.
4. Monetary calculations MUST use `MoneyUtils` — never use raw `double` arithmetic.
5. Sensitive data at rest MUST be encrypted via `EncryptionService`.
6. `BusinessId` subclasses MUST be `@Embeddable` and provide a protected no-arg constructor for JPA.
7. `BusinessId` values MUST be immutable after creation.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Using `String` for business identifiers | Create a `BusinessId` subclass (e.g., `OrderId`, `CustomerId`) |
| Using `double` for monetary amounts | Use `BigDecimal` with `MoneyUtils` |
| Manual `if (x == null) throw new ...` checks | Use `Check.notNull()`, `Check.notEmpty()`, etc. |
| Catching `IOException` everywhere | Use `UncheckedIO.run()` to convert to unchecked |
| Using persistence ID (`PersistenceEntity.id`) as business key | Use `BusinessId` for external references |
| Rolling your own encryption | Use the provided `EncryptionService` |
