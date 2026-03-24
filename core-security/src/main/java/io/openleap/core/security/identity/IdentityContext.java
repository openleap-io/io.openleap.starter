package io.openleap.core.security.identity;

import io.openleap.core.common.identity.IdentityHolder;

import java.util.Set;
import java.util.UUID;

public record IdentityContext(
        UUID tenantId,
        UUID userId,
        UUID principalId,
        Set<String> roles,
        Set<String> scopes
) {

    public static IdentityContext fromIdentityHolder() {
        return new IdentityContext(
                IdentityHolder.getTenantId(),
                IdentityHolder.getUserId(),
                IdentityHolder.getPrincipalId(),
                IdentityHolder.getRoles(),
                IdentityHolder.getScopes());
    }


}