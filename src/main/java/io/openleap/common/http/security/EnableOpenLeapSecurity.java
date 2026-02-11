package io.openleap.common.http.security;

import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan(basePackages = "io.openleap.common.http.security")
public @interface EnableOpenLeapSecurity {
}
