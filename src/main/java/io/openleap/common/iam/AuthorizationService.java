package io.openleap.common.iam;

import io.openleap.common.iam.dto.CheckPermissionRequestDto;
import io.openleap.common.iam.dto.CheckPermissionResponseDto;
import io.openleap.common.http.security.identity.IdentityHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthorizationService {

    private final IamAuthzClient iamAuthzClient;

    public AuthorizationService(IamAuthzClient iamAuthzClient) {
        this.iamAuthzClient = iamAuthzClient;
    }

    public void check(String permission) {
        UUID userId = IdentityHolder.getPrincipalId();
        CheckPermissionRequestDto request = new CheckPermissionRequestDto(userId, permission, null, null);
        CheckPermissionResponseDto response = iamAuthzClient.check(request);

        if (!response.allowed()) {
            throw new PermissionDeniedException(response);
        }
    }

    public void check(String permission, UUID resourceId, Map<String, Object> context) {
        UUID userId = IdentityHolder.getUserId();
        CheckPermissionRequestDto request = new CheckPermissionRequestDto(userId, permission, resourceId, context);
        CheckPermissionResponseDto response = iamAuthzClient.check(request);

        if (!response.allowed()) {
            throw new PermissionDeniedException(response);
        }
    }
}
