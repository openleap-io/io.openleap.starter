package io.openleap.common.iam.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CheckPermissionResponseDto(
        boolean allowed,
        String reason,
        String source,
        @JsonAlias("evaluationTimeMs") Long evaluationTimeMs
) {
}
