package io.openleap.starter.core.security.identity;

import java.util.Set;
import java.util.UUID;

public record IdentityContext(
        UUID tenantId,
        UUID userId,
        Set<String> roles,
        Set<String> scopes
) {

}