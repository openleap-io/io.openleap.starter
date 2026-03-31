package io.openleap.core.iam.config;

import io.openleap.core.iam.AuthorizationService;
import io.openleap.core.iam.aspect.PermissionCheckAspect;
import io.openleap.core.iam.client.IamAuthzClient;
import io.openleap.core.web.client.AuthClientHttpRequestInterceptor;
import io.openleap.core.web.client.LoggingClientHttpRequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.iam", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IamProperties.class)
public class IamAutoConfiguration {

    @Bean
    IamAuthzClient iamAuthzClient(IamProperties iamProperties,
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


    @Bean
    AuthorizationService authorizationService(IamAuthzClient iamAuthzClient) {
        return new AuthorizationService(iamAuthzClient);
    }

    @Bean
    PermissionCheckAspect permissionCheckAspect(AuthorizationService authorizationService) {
        return new PermissionCheckAspect(authorizationService);
    }

}
