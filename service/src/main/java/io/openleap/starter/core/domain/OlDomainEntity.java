package io.openleap.starter.core.domain;

/**
 * Interface marking a domain entity that has a typed business identifier.
 * This enforces the domain invariant that every domain entity must have
 * a non-null business identifier. The ID type is generic to support
 * typed identifiers (e.g., RoleId, UserId, TenantId) for type safety.
 */
public interface OlDomainEntity<ID> {
    ID getBusinessId();
}
