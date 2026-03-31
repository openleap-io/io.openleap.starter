package io.openleap.core.web.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// TODO: (itaseski): Request/response body/headers logging is unmasked — sensitive fields are exposed in plain text.
//  Restrict to development environments or implement field-level masking before enabling in production.
@Slf4j
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        logRequest(request, body);

        // TODO (itaseski): Consider adding time metrics.
        ClientHttpResponse response = execution.execute(request, body);

        return logAndBufferResponse(response, request);
    }

    private void logRequest(HttpRequest request, byte[] body) {
        log.debug("[HTTP →] {} {}", request.getMethod(), request.getURI());
        logHeaders("→", request.getHeaders());
        logBody("→", body);
    }

    private ClientHttpResponse logAndBufferResponse(ClientHttpResponse response,
                                                    HttpRequest request) throws IOException {
        byte[] body = response.getBody().readAllBytes();
        log.debug("[HTTP ←] {} {}", response.getStatusCode().value(), request.getURI());
        logHeaders("←", response.getHeaders());
        logBody("←", body);
        return new BufferedClientHttpResponse(response, body);
    }

    private void logHeaders(String direction, HttpHeaders headers) {
        headers.forEach((name, values) ->
                log.debug("[HTTP {}] Header: {}: {}", direction, name, values));
    }

    private void logBody(String direction, byte[] body) {
        if (body == null || body.length == 0) {
            return;
        }
        // TODO (itaseski): Add configurable value for truncating the body
        log.debug("[HTTP {}] Body: {}", direction, new String(body, StandardCharsets.UTF_8));
    }
}
