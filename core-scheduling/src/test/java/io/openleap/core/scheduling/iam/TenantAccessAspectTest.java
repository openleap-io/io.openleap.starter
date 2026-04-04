package io.openleap.core.scheduling.iam;

import io.openleap.core.scheduling.api.exception.TaskNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAccessAspectTest {

    @Mock
    private TaskAuthorizationService authorizationService;

    private TenantAccessAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new TenantAccessAspect(authorizationService);
    }

    @Test
    void checkAccess_callsHasAccessAndProceeds_whenTaskIdParamFound() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuthorizeTenantAccess annotation = mock(AuthorizeTenantAccess.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"taskId"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"tenant-1/abc"});
        when(annotation.taskId()).thenReturn("taskId");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.checkAccess(joinPoint, annotation);

        assertEquals("result", result);
        verify(authorizationService, times(1)).hasAccess("tenant-1/abc");
    }

    @Test
    void checkAccess_throwsTaskNotFoundException_whenAuthorizationFails() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuthorizeTenantAccess annotation = mock(AuthorizeTenantAccess.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"taskId"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"other-tenant/abc"});
        when(annotation.taskId()).thenReturn("taskId");
        doThrow(new TaskNotFoundException("other-tenant/abc")).when(authorizationService).hasAccess("other-tenant/abc");

        assertThrows(TaskNotFoundException.class, () -> aspect.checkAccess(joinPoint, annotation));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void checkAccess_proceedsWithoutAuthorization_whenTaskIdParamNotFound() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        AuthorizeTenantAccess annotation = mock(AuthorizeTenantAccess.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"handlerName"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"handler"});
        when(annotation.taskId()).thenReturn("taskId");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.checkAccess(joinPoint, annotation);

        assertEquals("result", result);
        verify(authorizationService, never()).hasAccess(any());
    }
}
