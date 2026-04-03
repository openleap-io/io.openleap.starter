package io.openleap.core.scheduling.web.dto;

import java.util.Map;

public record HandlerInfoResponse(
        String name,
        String payloadType,
        Map<String, String> payloadFields,
        String resultType,
        Map<String, String> resultFields
) {}
