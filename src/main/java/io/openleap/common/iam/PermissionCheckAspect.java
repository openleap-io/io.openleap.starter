package io.openleap.common.iam;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
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
