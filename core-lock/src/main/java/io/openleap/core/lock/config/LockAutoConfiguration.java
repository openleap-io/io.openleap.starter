package io.openleap.core.lock.config;

import io.openleap.core.lock.aspect.DistributedLockAspect;
import io.openleap.core.lock.db.LockRepository;
import io.openleap.core.lock.db.PostgresLockRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LockAutoConfiguration {

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
