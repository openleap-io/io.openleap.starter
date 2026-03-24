package io.openleap.core.web.config;

import io.openleap.core.web.client.ClientHttpRequestInterceptorConfig;
import io.openleap.core.web.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ClientHttpRequestInterceptorConfig.class, GlobalExceptionHandler.class})
public class WebAutoConfiguration {


}