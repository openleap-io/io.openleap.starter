package io.openleap.core.scheduling;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;

@SpringBootApplication(exclude = {
        // TODO (itaseski): Something somewhere is trying to autoconfigure the datasource.
        //  This is a workaround to prevent that from happening, but ideally we should figure out what it is and fix it.
        //  @EnableJpaAuditing is also causing issues since it is getting picked up
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        // TODO (itaseski): Needed since spring-security is also being autoconfigured.
        //  We can probably also solve this by enabling nosec from the core-securilty module
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
public class TestSchedulingApplication {
}
