package com.redhat.service.bridge.rhoas.resourcemanager;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

@ApplicationScoped
public class MasSSOMockServerConfigurator {

    public static final String TEST_ACCESS_TOKEN = "TESTACCESSTOKEN123";

    private static final String TEST_BODY = "{" +
            "\"access_token\":\"" + TEST_ACCESS_TOKEN + "\"," +
            "\"expires_in\":300," +
            "\"refresh_expires_in\":0," +
            "\"token_type\":\"Bearer\"," +
            "\"not-before-policy\": 0," +
            "\"scope\":\"profile email\"" +
            "}";

    @ConfigProperty(name = "mock-server.sso.mas.base-path")
    String basePath;
    @ConfigProperty(name = "mock-server.sso.mas.client-id")
    String clientId;
    @ConfigProperty(name = "mock-server.sso.mas.client-secret")
    String clientSecret;

    public void configure(WireMockServer server) {
        server.stubFor(post(pathOf("/token"))
                .withHeader("Content-Type", matching("application/x-www-form-urlencoded"))
                .withRequestBody(matching("grant_type=client_credentials&scope=email"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TEST_BODY)));
    }

    public String pathOf(String subPath) {
        return basePath + subPath;
    }

}
