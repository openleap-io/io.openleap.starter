package io.openleap.common.lock;

import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan(basePackages = "io.openleap.common.lock")
public @interface EnableOpenLeapDistributedLocking {
}
