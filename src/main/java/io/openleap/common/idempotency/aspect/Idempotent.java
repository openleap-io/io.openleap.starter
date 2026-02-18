package io.openleap.common.idempotency.aspect;

import java.lang.annotation.*;

/**
 * Marks a method as idempotent. Prevents duplicate execution based on a resolved command key.
 * The key can be a static string or a SpEL expression derived from method parameters.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Defines a static key for idempotency checks.
     */
    String key() default "";

    /**
     * Defines the key as a SpEL expression resolved from method parameters.
     */
    String keyExpression() default "";

    /**
     * Optional purpose/description stored alongside the idempotency record.
     */
    String purpose() default  "";

    /**
     * In case the command was already processed:
     * if true, a DuplicateCommandException will be thrown,
     * if false, execution will be silently skipped. By default, set to false.
     */
    boolean failOnDuplicateExecution() default false;

}