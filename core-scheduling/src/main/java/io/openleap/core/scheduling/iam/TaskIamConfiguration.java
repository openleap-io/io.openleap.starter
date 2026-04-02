package io.openleap.core.scheduling.iam;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO (itaseski): Consider moving all configuration classes in different domains under .config
@Configuration
public class TaskIamConfiguration {

    @Bean
    public TaskAuthorizationService taskAuthorizationService() {
        return new TaskAuthorizationService();
    }

    @Bean
    public TenantAccessAspect tenantAccessAspect(TaskAuthorizationService authorizationService) {
        return new TenantAccessAspect(authorizationService);
    }
}
