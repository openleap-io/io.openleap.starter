package io.openleap.core.scheduling.iam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO (itaseski): Consider introducing another @TenantScoped annotation for classes that are tenant-scoped
//  but don't have a taskId parameter. E.g. listTasks per tenant.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizeTenantAccess {
    String taskId() default "taskId";
}
