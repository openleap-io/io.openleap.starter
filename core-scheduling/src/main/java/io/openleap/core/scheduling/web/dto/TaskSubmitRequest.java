package io.openleap.core.scheduling.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record TaskSubmitRequest(
        @NotNull JsonNode payload,
        @Size(max = 255) String deduplicationKey,
        @Min(1) Integer priority,
        @Positive Long timeoutSeconds
) {}
