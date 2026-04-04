package io.openleap.core.extension.config;

import io.openleap.core.extension.registry.CustomFieldRegistry;
import io.openleap.core.extension.spi.CustomFieldProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.extension", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ExtensionProperties.class)
public class ExtensionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CustomFieldRegistry customFieldRegistry(List<CustomFieldProvider<?>> providers) {
        return new CustomFieldRegistry(providers);
    }
}
