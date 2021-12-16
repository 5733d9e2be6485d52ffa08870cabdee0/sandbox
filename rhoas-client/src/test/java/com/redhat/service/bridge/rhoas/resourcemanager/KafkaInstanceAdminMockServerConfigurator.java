package com.redhat.service.bridge.rhoas.resourcemanager;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.redhat.service.bridge.rhoas.dto.Topic;
import com.redhat.service.bridge.rhoas.dto.Topics;

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

    public void configureWithEmptyTestTopic(WireMockServer server) {
        server.stubFor(authGet("/topics").willReturn(responseWithEmptyTopics()));
        server.stubFor(authPost("/topics").willReturn(responseWithCreatedTopic()));
        server.stubFor(authPost("/acls").willReturn(emptyResponse(200)));
    }

    public void configureWithExistingTestTopic(WireMockServer server) {
        server.stubFor(authGet("/topics").willReturn(responseWithExistingTopic()));
        server.stubFor(authPost("/topics").willReturn(emptyResponse(409)));
        server.stubFor(authPost("/acls").willReturn(emptyResponse(200)));
    }

    private ResponseDefinitionBuilder responseWithEmptyTopics() {
        Topics body = new Topics();
        body.setPage(1);
        body.setSize(0);
        body.setTotal(0);
        body.setItems(Collections.emptyList());
        return jsonResponse(body);
    }

    private ResponseDefinitionBuilder responseWithExistingTopic() {
        Topic testTopic = new Topic();
        testTopic.setName(TEST_TOPIC_NAME);
        testTopic.setInternal(false);

        Topics body = new Topics();
        body.setPage(1);
        body.setSize(1);
        body.setTotal(0);
        body.setItems(Collections.singletonList(testTopic));

        return jsonResponse(body);
    }

    private ResponseDefinitionBuilder responseWithCreatedTopic() {
        Topic body = new Topic();
        body.setName(TEST_TOPIC_NAME);
        return jsonResponse(body);
    }

}
