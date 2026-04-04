package io.openleap.core.scheduling.iam;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class TenantAccessAspect {

    private final TaskAuthorizationService authorizationService;

    public TenantAccessAspect(TaskAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Around("@annotation(authorizeTenantAccess)")
    public Object checkAccess(ProceedingJoinPoint joinPoint, AuthorizeTenantAccess authorizeTenantAccess) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(authorizeTenantAccess.taskId())) {
                authorizationService.hasAccess((String) args[i]);
                break;
            }
        }

        return joinPoint.proceed();
    }
}
