package io.openleap.starter.testkit.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockTestSupport {

    private WireMockTestSupport() {

    }

    // --- GET ---
    public static WireMockServer stubGetJson(String path, String responseBody) {
        return startAndStub(get(urlEqualTo(path)), responseBody, 200);
    }

    // --- POST ---
    public static WireMockServer stubPostJson(String path, String responseBody) {
        return startAndStub(post(urlEqualTo(path)), responseBody, 201);
    }

    // --- PUT ---
    public static WireMockServer stubPutJson(String path, String responseBody) {
        return startAndStub(put(urlEqualTo(path)), responseBody, 200);
    }

    // --- PATCH ---
    public static WireMockServer stubPatchJson(String path, String responseBody) {
        return startAndStub(patch(urlEqualTo(path)), responseBody, 200);
    }

    private static WireMockServer startAndStub(MappingBuilder mappingBuilder, String responseBody, int status) {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();

        ResponseDefinitionBuilder response = aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody);

        server.stubFor(mappingBuilder.willReturn(response));

        return server;
    }

}
