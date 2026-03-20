package io.openleap.core.iam.config;

import io.openleap.core.iam.client.IamAuthzClient;
import io.openleap.core.web.client.AuthClientHttpRequestInterceptor;
import io.openleap.core.web.client.LoggingClientHttpRequestInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@EnableConfigurationProperties(IamProperties.class)
@Configuration
public class IamConfig {

    @Bean
    public IamAuthzClient iamAuthzClient(IamProperties iamProperties,
                                         AuthClientHttpRequestInterceptor authInterceptor,
                                         LoggingClientHttpRequestInterceptor loggingInterceptor) {

        RestClient restClient = RestClient.builder()
                .baseUrl(iamProperties.getAuthzBaseUrl())
                .requestInterceptor(authInterceptor)
                .requestInterceptor(loggingInterceptor)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(IamAuthzClient.class);
    }

}