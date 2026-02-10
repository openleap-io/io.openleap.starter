package io.openleap.common.idempotency;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan(basePackages = "io.openleap.common.idempotency")
@EntityScan(basePackages = "io.openleap.common.idempotency")
@EnableJpaRepositories(basePackages = "io.openleap.common.idempotency")
public @interface EnableOpenLeapIdempotency {
}
