# ADR-005: Entity Persistence & JPA Auditing

**Status:** Accepted

## Context

Microservices need a consistent entity model with common concerns: primary keys, optimistic locking, and audit trails (who created/modified a record and when). These concerns cut across all domain entities and must integrate with the identity system (see [ADR-001](ADR-001-identity-security-context.md)) so that `createdBy` and `lastModifiedBy` are automatically populated from the current security context.

## Decision

The starter provides a three-level entity hierarchy and JPA auditing integration that automatically fills audit fields from `IdentityHolder`.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `PersistenceEntity` | `io.openleap.common.persistence.entity.PersistenceEntity` | Base entity with `id` (UUID) |
| `VersionedEntity` | `io.openleap.common.persistence.entity.VersionedEntity` | Extends `PersistenceEntity`; adds `@Version` for optimistic locking |
| `AuditableEntity` | `io.openleap.common.persistence.entity.AuditableEntity` | Extends `VersionedEntity`; adds `createdAt`, `createdBy`, `lastModifiedAt`, `lastModifiedBy` |
| `SpecificationBuilder<T>` | `io.openleap.common.persistence.specification.SpecificationBuilder` | Fluent builder for JPA `Specification` queries |
| `JpaAuditingConfig` | `io.openleap.common.persistence.config.JpaAuditingConfig` | Enables JPA auditing via `@EnableJpaAuditing` |
| `AuditingProviderConfig` | `io.openleap.common.persistence.config.AuditingProviderConfig` | Provides `AuditorAware` linked to `IdentityHolder.getUserId()` |
| `TenantRlsAspect` | `io.openleap.common.persistence.config.TenantRlsAspect` | Row-level security aspect for tenant isolation |
| `DomainEntity` | `io.openleap.common.domain.DomainEntity` | Base interface for domain entities (business identity) |

### Entity Hierarchy

```
PersistenceEntity          (id: UUID)
    └── VersionedEntity    (version: Long — @Version)
        └── AuditableEntity (createdAt, createdBy, lastModifiedAt, lastModifiedBy)
```

### Auditing Flow

```
1. Entity is persisted or updated
2. JPA @EntityListeners triggers AuditingEntityListener
3. AuditingProviderConfig provides AuditorAware<String>
4. AuditorAware reads IdentityHolder.getUserId()
5. createdBy / lastModifiedBy are set automatically
```

## Usage

### Defining a Domain Entity

```java
@Entity
@Table(name = "orders")
public class Order extends AuditableEntity implements DomainEntity<OrderId> {

    @Embedded
    private OrderId businessId;

    @Column(nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Inherits: id, version, createdAt, createdBy, lastModifiedAt, lastModifiedBy

    @Override
    public OrderId getBusinessId() {
        return businessId;
    }
}
```

### Using SpecificationBuilder for Dynamic Queries

```java
Specification<Order> spec = SpecificationBuilder.<Order>create()
    .equal("status", OrderStatus.PENDING)
    .like("customerName", searchTerm)
    .greaterThan("createdAt", startDate)
    .in("region", List.of("EU", "US"))
    .build();

Page<Order> results = orderRepository.findAll(spec, pageable);
```

### Enabling Auditing

```java
@SpringBootApplication
@EnableOpenLeapAuditingJpa
public class MyServiceApplication { }
```

## Configuration

JPA auditing is activated by the `@EnableOpenLeapAuditingJpa` annotation. No additional `application.yml` properties are required beyond standard JPA/Hibernate settings.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

## Compliance Rules

1. Domain entities MUST extend `AuditableEntity` (or at minimum `PersistenceEntity`).
2. Entities with concurrent access MUST extend `VersionedEntity` or `AuditableEntity` to enable optimistic locking.
3. `@EnableOpenLeapAuditingJpa` MUST be present for audit fields to be populated.
4. Entities MUST NOT manually set `createdBy` or `lastModifiedBy` — these are auto-populated from `IdentityHolder`.
5. Entities implementing `DomainEntity<T>` MUST provide a `BusinessId`-based identity separate from the persistence `id`.
6. Dynamic queries SHOULD use `SpecificationBuilder` instead of manual JPQL or native SQL for type safety.
7. `@Version` MUST NOT be manually incremented — Hibernate manages it.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Creating entities without extending the hierarchy | Extend `AuditableEntity` for full feature set |
| Manually setting `createdBy` / `lastModifiedBy` | Let JPA auditing + `IdentityHolder` handle it |
| Using `ddl-auto: create` or `update` in production | Use `validate` with Flyway migrations |
| Writing raw JPQL for filterable list queries | Use `SpecificationBuilder` for composable, type-safe queries |
| Catching `OptimisticLockException` and retrying silently | Surface version conflicts to the caller |
| Using database-generated IDs as business identifiers | Use `BusinessId` for external references, `PersistenceEntity.id` for internal |
