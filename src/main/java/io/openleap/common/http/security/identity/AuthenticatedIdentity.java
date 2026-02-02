package io.openleap.common.http.security.identity;

import java.lang.annotation.*;

/**
 * Annotation to inject the current multi-tenant identity context into Controller methods.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthenticatedIdentity {
}
