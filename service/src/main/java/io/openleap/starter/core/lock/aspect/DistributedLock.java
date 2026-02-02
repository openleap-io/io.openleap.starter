package io.openleap.starter.core.lock.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides concurrency control for the execution of the annotated method across multiple application instances.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedLock {

    /**
     * Defines the key of the operation for which the lock will be acquired.
     */
    String key() default "";

    /**
     * Defines the key SPEL expression of the operation for which the lock will be acquired.
     */
    String keyExpression() default "";

    /**
     * In case the process fails to acquire the lock:
     * if true, a ConcurrentExecutionException will be thrown,
     * if false, execution will be skipped. By default, set to false.
     */
    boolean failOnConcurrentExecution() default false;

}
