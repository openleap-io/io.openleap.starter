package io.openleap.core.persistence.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({JpaAuditingConfig.class, AuditingProviderConfig.class})
public class PersistenceAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public TenantRlsAspect tenantRlsAspect(JdbcTemplate jdbcTemplate) {
        return new TenantRlsAspect(jdbcTemplate);
    }

}