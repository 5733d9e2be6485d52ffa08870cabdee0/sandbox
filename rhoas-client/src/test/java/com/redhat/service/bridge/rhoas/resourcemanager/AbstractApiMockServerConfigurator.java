package com.redhat.service.bridge.rhoas.resourcemanager;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;

public abstract class AbstractApiMockServerConfigurator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String intermediatePath;
    private final String expectedAccessToken;

    public AbstractApiMockServerConfigurator(String intermediatePath, String expectedAccessToken) {
        this.intermediatePath = intermediatePath;
        this.expectedAccessToken = expectedAccessToken;
    }

    protected abstract String getBasePath();

    private MappingBuilder auth(Function<String, MappingBuilder> method, String subPath) {
        return method.apply(pathOf(subPath)).withHeader("Authorization", matching("Bearer " + expectedAccessToken));
    }

    protected MappingBuilder authGet(String subPath) {
        return auth(WireMock::get, subPath);
    }

    protected MappingBuilder authPost(String subPath) {
        return auth(WireMock::post, subPath);
    }

    protected String pathOf(String subPath) {
        return getBasePath() + intermediatePath + subPath;
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
                .withBody(bodyString);
    }

    protected ResponseDefinitionBuilder emptyResponse(int status) {
        return WireMock.aResponse().withStatus(status);
    }
}
