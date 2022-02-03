package com.redhat.service.bridge.test.rhoas;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContentPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

@ApplicationScoped
public class RedHatSSOMockServerConfigurator {

    public static final String TEST_ACCESS_TOKEN = "TESTACCESSTOKEN456";

    private static final String TEST_BODY = "{" +
            "\"access_token\":\"" + TEST_ACCESS_TOKEN + "\"," +
            "\"expires_in\":300," +
            "\"refresh_expires_in\":0," +
            "\"token_type\":\"Bearer\"," +
            "\"not-before-policy\": 0," +
            "\"scope\":\"openid offline_access\"" +
            "}";

    @ConfigProperty(name = "rhoas-mock-server.sso.red-hat.base-path")
    String basePath;

    @ConfigProperty(name = "rhoas-mock-server.sso.red-hat.refresh-token")
    String refreshToken;

    public void configure(WireMockServer server) {
        server.stubFor(post(pathOf("/token"))
                .withHeader("Content-Type", matching("application/x-www-form-urlencoded"))
                .withRequestBody(bodyMatcher())
                .willReturn(response()));
    }

    public String pathOf(String subPath) {
        return basePath + subPath;
    }

    private ContentPattern<?> bodyMatcher() {
        return matching("grant_type=refresh_token&refresh_token=" + refreshToken);
    }

    private ResponseDefinitionBuilder response() {
        return WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TEST_BODY);
    }

}
