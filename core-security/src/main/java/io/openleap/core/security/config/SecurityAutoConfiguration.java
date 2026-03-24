package io.openleap.core.security.config;

import io.openleap.core.security.CustomJwtGrantedAuthoritiesConverter;
import io.openleap.core.security.NosecHeaderFilter;
import io.openleap.core.security.SecurityKeycloakConfig;
import io.openleap.core.security.SecurityLoggerConfig;
import io.openleap.core.security.identity.IdentityContextArgumentResolver;
import io.openleap.core.security.identity.IdentityHttpFilter;
import io.openleap.core.security.identity.IdentityWebConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
@Import({SecurityKeycloakConfig.class, SecurityLoggerConfig.class, IdentityWebConfig.class})
public class SecurityAutoConfiguration {

    @Bean
    public IdentityHttpFilter identityHttpFilter(Optional<SecurityProperties> securityProperties) {
        return new IdentityHttpFilter(securityProperties);
    }

    @Bean
    @Profile("nosec")
    public NosecHeaderFilter nosecHeaderFilter() {
        return new NosecHeaderFilter();
    }

    @Bean
    public IdentityContextArgumentResolver identityContextArgumentResolver() {
        return new IdentityContextArgumentResolver();
    }

    @Bean
    public CustomJwtGrantedAuthoritiesConverter customJwtGrantedAuthoritiesConverter() {
        return new CustomJwtGrantedAuthoritiesConverter();
    }

}
