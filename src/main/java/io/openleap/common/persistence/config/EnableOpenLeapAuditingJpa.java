package io.openleap.common.persistence.config;

import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan(basePackages = "io.openleap.common.persistence.config")
public @interface EnableOpenLeapAuditingJpa {
}
