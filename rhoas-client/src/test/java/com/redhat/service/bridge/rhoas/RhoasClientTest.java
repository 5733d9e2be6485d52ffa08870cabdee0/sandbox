package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;
import com.redhat.service.bridge.test.rhoas.RhoasMockServerResource;
import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

import static com.redhat.service.bridge.test.rhoas.KafkaInstanceAdminMockServerConfigurator.TEST_TOPIC_NAME;
import static com.redhat.service.bridge.test.rhoas.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_ID;

@QuarkusTest
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
@TestProfile(RhoasEnabledTestProfile.class)
class RhoasClientTest extends RhoasTestBase {

    private static final NewTopicInput TEST_TOPIC_INPUT = new NewTopicInput()
            .name(TEST_TOPIC_NAME)
            .settings(new TopicSettings().numPartitions(1));

    @Inject
    RhoasClient rhoasClient;

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerOk() {
        configureMockAPIWithAllWorking();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, 1, 3, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, 1, 0, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, 1, 3, 1, 0);
    }
    //
    //    @Test
    //    void testCreateTopicAndConsumerServiceAccountWithBrokenTopicCreation() {
    //        configureMockAPIWithBrokenACLCreation();
    //
    //        rhoasClient.createTopicAndConsumerServiceAccount(new TopicAndServiceAccountRequest(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
    //                .subscribe().withSubscriber(UniAssertSubscriber.create())
    //                .awaitFailure(Duration.ofSeconds(60));
    //
    //        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
    //        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
    //        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));
    //
    //        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
    //        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
    //        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    //    }
    //
    //    @Test
    //    void testCreateTopicAndConsumerServiceAccountWithBrokenACLCreation() {
    //        configureMockAPIWithBrokenACLCreation();
    //
    //        rhoasClient.createTopicAndConsumerServiceAccount(new TopicAndServiceAccountRequest(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
    //                .subscribe().withSubscriber(UniAssertSubscriber.create())
    //                .awaitFailure(Duration.ofSeconds(60));
    //
    //        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
    //        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
    //        wireMockServer.verify(3, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));
    //
    //        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
    //        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
    //        wireMockServer.verify(3, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    //    }

    private void testCreateTopicAndGrantAccess(RhoasTopicAccessType accessType, int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        rhoasClient.createTopicAndGrantAccess(TEST_TOPIC_INPUT, TEST_SERVICE_ACCOUNT_ID, accessType)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(60));
        verifyWireMockServer(expectedPostTopics, expectedPostACLs, expectedDeleteTopics, expectedDeleteACLs);
    }

    private void testDeleteTopicAndRevokeAccess(RhoasTopicAccessType accessType, int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        rhoasClient.deleteTopicAndRevokeAccess(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_ID, accessType)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(60));
        verifyWireMockServer(expectedPostTopics, expectedPostACLs, expectedDeleteTopics, expectedDeleteACLs);
    }

    private void verifyWireMockServer(int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        wireMockServer.verify(expectedPostTopics, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        //        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(expectedPostACLs, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(expectedDeleteTopics, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        //        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(expectedDeleteACLs, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

}
