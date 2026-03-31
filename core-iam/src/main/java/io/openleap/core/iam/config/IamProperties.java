package io.openleap.core.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ol.iam")
public class IamProperties {

    private String authzBaseUrl = "http://iam-authz-svc:8082";

    public String getAuthzBaseUrl() {
        return authzBaseUrl;
    }

    public void setAuthzBaseUrl(String authzBaseUrl) {
        this.authzBaseUrl = authzBaseUrl;
    }
}
