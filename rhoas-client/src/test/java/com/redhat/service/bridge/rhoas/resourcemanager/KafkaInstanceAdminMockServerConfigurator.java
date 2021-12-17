package com.redhat.service.bridge.rhoas.resourcemanager;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.redhat.service.bridge.rhoas.dto.Topic;

import static com.redhat.service.bridge.rhoas.resourcemanager.MasSSOMockServerConfigurator.TEST_ACCESS_TOKEN;

@ApplicationScoped
public class KafkaInstanceAdminMockServerConfigurator extends AbstractApiMockServerConfigurator {

    public static final String TEST_TOPIC_NAME = "test-topic";

    @ConfigProperty(name = "mock-server.instance-api.base-path")
    String basePath;

    public KafkaInstanceAdminMockServerConfigurator() {
        super("/rest", TEST_ACCESS_TOKEN);
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }

    public void configureWithAllWorking(WireMockServer server) {
        server.stubFor(authPost("/topics").willReturn(responseWithCreatedTopic()));
        server.stubFor(authDelete("/topics/" + TEST_TOPIC_NAME).willReturn(responseWithStatus(200)));

        server.stubFor(authPost("/acls").willReturn(responseWithStatus(201)));
        server.stubFor(authDelete("/acls?.*").willReturn(responseWithStatus(200)));
    }

    public void configureWithBrokenTopicCreation(WireMockServer server) {
        server.stubFor(authPost("/topics").willReturn(responseWithStatus(500)));
        server.stubFor(authDelete("/topics/" + TEST_TOPIC_NAME).willReturn(responseWithStatus(200)));

        server.stubFor(authPost("/acls").willReturn(responseWithStatus(201)));
        server.stubFor(authDelete("/acls?.*").willReturn(responseWithStatus(200)));
    }

    public void configureWithBrokenACLCreation(WireMockServer server) {
        server.stubFor(authPost("/topics").willReturn(responseWithCreatedTopic()));
        server.stubFor(authDelete("/topics/" + TEST_TOPIC_NAME).willReturn(responseWithStatus(200)));

        server.stubFor(authPost("/acls").willReturn(responseWithStatus(500)));
        server.stubFor(authDelete("/acls?.*").willReturn(responseWithStatus(200)));
    }

    private ResponseDefinitionBuilder responseWithCreatedTopic() {
        Topic body = new Topic();
        body.setName(TEST_TOPIC_NAME);
        return jsonResponse(body);
    }

}
