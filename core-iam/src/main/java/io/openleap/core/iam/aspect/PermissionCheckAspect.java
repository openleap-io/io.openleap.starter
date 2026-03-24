package io.openleap.core.iam.aspect;

import io.openleap.core.iam.AuthorizationService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class PermissionCheckAspect {

    private final AuthorizationService authorizationService;

    public PermissionCheckAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Before(value = "@annotation(requiresPermission)", argNames = "requiresPermission")
    public void before(RequiresPermission requiresPermission) {
        authorizationService.check(requiresPermission.value());
    }
}
