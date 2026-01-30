package io.openleap.starter.core.lock.config;

import io.openleap.starter.core.lock.aspect.DistributedLockAspect;
import io.openleap.starter.core.lock.db.LockRepository;
import io.openleap.starter.core.lock.db.PostgresLockRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

// Spring specific configuration
@Configuration
public class DistributedLockConfig {

    @Bean
    public LockRepository lockRepository() {
        return new PostgresLockRepository();
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public DistributedLockAspect distributedLockAspect(DataSource dataSource, LockRepository lockRepository) {
        return new DistributedLockAspect(dataSource, lockRepository);
    }

}
