package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.inject.Inject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

import static com.redhat.service.bridge.test.rhoas.KafkaInstanceAdminMockServerConfigurator.TEST_TOPIC_NAME;
import static com.redhat.service.bridge.test.rhoas.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_ID;

abstract class RhoasClientTestBase extends RhoasTestBase {

    private static final int TIMEOUT_SECONDS = 20;
    private static final NewTopicInput TEST_TOPIC_INPUT = new NewTopicInput()
            .name(TEST_TOPIC_NAME)
            .settings(new TopicSettings().numPartitions(1));

    @Inject
    RhoasClient rhoasClient;

    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    protected void testCreateTopicAndGrantAccess(RhoasTopicAccessType accessType, boolean expectFailure, int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics,
            int expectedDeleteACLs) {
        UniAssertSubscriber<?> subscriber = rhoasClient.createTopicAndGrantAccess(TEST_TOPIC_INPUT, TEST_SERVICE_ACCOUNT_ID, accessType)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        if (expectFailure) {
            subscriber.awaitFailure(Duration.ofSeconds(TIMEOUT_SECONDS));
        } else {
            subscriber.awaitItem(Duration.ofSeconds(TIMEOUT_SECONDS));
        }

        verifyWireMockServer(expectedPostTopics, expectedPostACLs, expectedDeleteTopics, expectedDeleteACLs);
    }

    protected void testDeleteTopicAndRevokeAccess(RhoasTopicAccessType accessType, boolean expectFailure, int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics,
            int expectedDeleteACLs) {
        UniAssertSubscriber<?> subscriber = rhoasClient.deleteTopicAndRevokeAccess(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_ID, accessType)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        if (expectFailure) {
            subscriber.awaitFailure(Duration.ofSeconds(TIMEOUT_SECONDS));
        } else {
            subscriber.awaitItem(Duration.ofSeconds(TIMEOUT_SECONDS));
        }

        verifyWireMockServer(expectedPostTopics, expectedPostACLs, expectedDeleteTopics, expectedDeleteACLs);
    }

    protected void verifyWireMockServer(int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        wireMockServer.verify(expectedPostTopics, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(expectedPostACLs, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(expectedDeleteTopics, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(expectedDeleteACLs, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }
}
