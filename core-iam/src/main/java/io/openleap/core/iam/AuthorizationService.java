package io.openleap.core.iam;

import io.openleap.core.common.exception.PermissionDeniedException;
import io.openleap.core.common.identity.IdentityHolder;
import io.openleap.core.iam.client.IamAuthzClient;
import io.openleap.core.iam.dto.CheckPermissionRequest;
import io.openleap.core.iam.dto.CheckPermissionResponse;

import java.util.Map;
import java.util.UUID;

public class AuthorizationService {

    private final IamAuthzClient iamAuthzClient;

    public AuthorizationService(IamAuthzClient iamAuthzClient) {
        this.iamAuthzClient = iamAuthzClient;
    }

    public void check(String permission) {
        UUID userId = IdentityHolder.getPrincipalId();

        CheckPermissionRequest request = new CheckPermissionRequest(userId, permission);

        CheckPermissionResponse response = iamAuthzClient.check(request);

        if (!response.allowed()) {
            // TODO (itaseski): Maybe use standard spring exceptions instead of custom
            throw new PermissionDeniedException(response.reason());
        }
    }

    public void check(String permission, UUID resourceId, Map<String, Object> context) {
        UUID userId = IdentityHolder.getUserId();

        CheckPermissionRequest request = new CheckPermissionRequest(userId, permission, resourceId, context);

        CheckPermissionResponse response = iamAuthzClient.check(request);

        if (!response.allowed()) {
            throw new PermissionDeniedException(response.reason());
        }
    }
}
