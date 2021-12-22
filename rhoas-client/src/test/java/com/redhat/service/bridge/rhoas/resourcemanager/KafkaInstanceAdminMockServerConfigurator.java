package com.redhat.service.bridge.rhoas.resourcemanager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

@ApplicationScoped
public class KafkaInstanceAdminMockServerConfigurator extends AbstractApiMockServerConfigurator {

    public static final String TEST_TOPIC_NAME = "test-topic";

    @ConfigProperty(name = "mock-server.instance-api.base-path")
    String basePath;

    public KafkaInstanceAdminMockServerConfigurator() {
        super("/rest", MasSSOMockServerConfigurator.TEST_ACCESS_TOKEN);
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }

    public void configureWithAllWorking(WireMockServer server) {
        server.stubFor(authPost("/topics").willReturn(responseWithCreatedTopic()));
        server.stubFor(authDelete("/topics/" + TEST_TOPIC_NAME).willReturn(responseWithStatus(200)));

        server.stubFor(authPost("/acls").willReturn(responseWithStatus(201)));
        server.stubFor(authDelete("/acls?.*").willReturn(responseWithDeletedACL()));
    }

    public void configureWithBrokenTopicCreation(WireMockServer server) {
        server.stubFor(authPost("/topics").willReturn(responseWithStatus(500)));
        server.stubFor(authDelete("/topics/" + TEST_TOPIC_NAME).willReturn(responseWithStatus(200)));

        server.stubFor(authPost("/acls").willReturn(responseWithStatus(201)));
        server.stubFor(authDelete("/acls?.*").willReturn(responseWithDeletedACL()));
    }

    public void configureWithBrokenACLCreation(WireMockServer server) {
        server.stubFor(authPost("/topics").willReturn(responseWithCreatedTopic()));
        server.stubFor(authDelete("/topics/" + TEST_TOPIC_NAME).willReturn(responseWithStatus(200)));

        server.stubFor(authPost("/acls").willReturn(responseWithStatus(500)));
        server.stubFor(authDelete("/acls?.*").willReturn(responseWithDeletedACL()));
    }

    private ResponseDefinitionBuilder responseWithCreatedTopic() {
        Map<String, String> body = new HashMap<>();
        body.put("name", TEST_TOPIC_NAME);
        return jsonResponse(body);
    }

    private ResponseDefinitionBuilder responseWithDeletedACL() {
        Map<String, Object> body = new HashMap<>();
        body.put("items", Collections.emptyList());
        body.put("total", 1);
        body.put("page", 1);
        body.put("size", 1);
        return jsonResponse(body);
    }

}
