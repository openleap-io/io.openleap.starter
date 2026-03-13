package io.openleap.common.iam;

import io.openleap.common.iam.dto.CheckPermissionResponseDto;

public class PermissionDeniedException extends RuntimeException {

    private final CheckPermissionResponseDto response;

    public PermissionDeniedException(CheckPermissionResponseDto response) {
        super(response.reason());
        this.response = response;
    }

    public CheckPermissionResponseDto getResponse() {
        return response;
    }
}
