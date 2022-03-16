package com.redhat.service.bridge.test.rhoas;

import java.util.function.Function;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public abstract class AbstractApiMockServerConfigurator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ConfigProperty(name = "rhoas-mock-server.delay", defaultValue = "250")
    int delay;

    private final String intermediatePath;
    private final String expectedAccessToken;

    protected AbstractApiMockServerConfigurator(String intermediatePath, String expectedAccessToken) {
        this.intermediatePath = intermediatePath;
        this.expectedAccessToken = expectedAccessToken;
    }

    public String pathOf(String subPath) {
        return getBasePath() + intermediatePath + subPath;
    }

    protected abstract String getBasePath();

    private MappingBuilder auth(Function<UrlPattern, MappingBuilder> method, String subPath) {
        return method.apply(urlMatching(pathOf(subPath))).withHeader("Authorization", matching("Bearer " + expectedAccessToken));
    }

    protected MappingBuilder authDelete(String subPath) {
        return auth(WireMock::delete, subPath);
    }

    protected MappingBuilder authPost(String subPath) {
        return auth(WireMock::post, subPath);
    }

    protected MappingBuilder authGet(String subPath) {
        return auth(WireMock::get, subPath);
    }

    protected ResponseDefinitionBuilder jsonResponse(Object body) {
        return jsonResponse(body, 200);
    }

    protected ResponseDefinitionBuilder jsonResponse(Object body, int status) {
        String bodyString = "{}";
        try {
            bodyString = MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return WireMock.aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(bodyString)
                .withFixedDelay(delay);
    }

    protected ResponseDefinitionBuilder responseWithStatus(int status) {
        return WireMock.aResponse()
                .withStatus(status)
                .withFixedDelay(delay);
    }
}
