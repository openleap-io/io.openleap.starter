package io.openleap.common.iam.dto;

import java.util.Map;
import java.util.UUID;

public record CheckPermissionRequestDto(
        UUID userId,
        String permission,
        UUID resourceId,
        Map<String, Object> context
) {
}
