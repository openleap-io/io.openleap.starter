package io.openleap.core.web.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClientHttpRequestInterceptorConfig {

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
    AuthClientHttpRequestInterceptor authClientHttpRequestInterceptor() {
        return new AuthClientHttpRequestInterceptor();
    }

    @Bean
    LoggingClientHttpRequestInterceptor loggingClientHttpRequestInterceptor() {
        return new LoggingClientHttpRequestInterceptor();
    }
}
