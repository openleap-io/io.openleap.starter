package io.openleap.core.iam.dto;

import java.util.Map;
import java.util.UUID;

public record CheckPermissionRequest(
        UUID userId,
        String permission,
        UUID resourceId,
        Map<String, Object> context
) {

    public CheckPermissionRequest(UUID userId, String permission) {
        this(userId, permission, null, null);
    }

}
