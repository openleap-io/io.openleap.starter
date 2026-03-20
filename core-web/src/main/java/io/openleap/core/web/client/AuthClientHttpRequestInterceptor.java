package io.openleap.core.web.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;

@Slf4j
public class AuthClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.warn("[AuthInterceptor] No authentication in security context for {} {} — skipping Authorization header",
                    request.getMethod(), request.getURI());
            return execution.execute(request, body);
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("[AuthInterceptor] Principal is not a JWT (was: {}) for {} {} — skipping Authorization header",
                    authentication.getPrincipal().getClass().getSimpleName(),
                    request.getMethod(), request.getURI());
            return execution.execute(request, body);
        }

        request.getHeaders().add(AUTHORIZATION_HEADER, AUTHORIZATION_BEARER_PREFIX + jwt.getTokenValue());

        return execution.execute(request, body);
    }
}
