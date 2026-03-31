package io.openleap.core.iam.dto;

public record CheckPermissionResponse(
        boolean allowed,
        String reason,
        String source,
        Long evaluationTimeMs
) {
}
