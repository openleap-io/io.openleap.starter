package io.openleap.common.messaging;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan(basePackages = "io.openleap.common.messaging")
@EntityScan(basePackages = "io.openleap.common.messaging")
@EnableJpaRepositories(basePackages = "io.openleap.common.messaging")
public @interface EnableOpenLeapMessaging {
}
