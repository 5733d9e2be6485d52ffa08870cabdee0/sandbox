package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
@Disabled
class RhoasClientTest extends RhoasTestBase {

    private static final int TIMEOUT_SECONDS = 20;
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
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, false, 1, 3, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, true, 1, 3, 0, 3);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, false, 1, 3, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, true, 0, 3, 0, 3);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerOk() {
        configureMockAPIWithAllWorking();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, false, 1, 5, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, true, 1, 5, 0, 5);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, false, 1, 5, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, true, 0, 5, 0, 5);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerOk() {
        configureMockAPIWithAllWorking();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 1, 8, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 1, 8, 0, 8);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 1, 8, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 0, 8, 0, 8);
    }

    // -------------

    @Test
    void testDeleteTopicAndRevokeAccessConsumerOk() {
        configureMockAPIWithAllWorking();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, false, 0, 0, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerWithBrokenTopicDeletion() {
        configureMockAPIWithBrokenTopicDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, true, 0, 3, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerWithBrokenServiceAccountDeletion() {
        configureMockAPIWithBrokenServiceAccountDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, false, 0, 0, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerWithBrokenACLDeletion() {
        configureMockAPIWithBrokenACLDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, true, 0, 3, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerOk() {
        configureMockAPIWithAllWorking();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, false, 0, 0, 1, 5);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerWithBrokenTopicDeletion() {
        configureMockAPIWithBrokenTopicDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, true, 0, 5, 1, 5);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerWithBrokenServiceAccountDeletion() {
        configureMockAPIWithBrokenServiceAccountDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, false, 0, 0, 1, 5);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerWithBrokenACLDeletion() {
        configureMockAPIWithBrokenACLDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, true, 0, 5, 1, 5);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerOk() {
        configureMockAPIWithAllWorking();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 0, 0, 1, 8);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithBrokenTopicDeletion() {
        configureMockAPIWithBrokenTopicDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 0, 8, 1, 8);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithBrokenServiceAccountDeletion() {
        configureMockAPIWithBrokenServiceAccountDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 0, 0, 1, 8);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithBrokenACLDeletion() {
        configureMockAPIWithBrokenACLDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 0, 8, 1, 8);
    }

    private void testCreateTopicAndGrantAccess(RhoasTopicAccessType accessType, boolean expectFalure, int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        UniAssertSubscriber<?> subscriber = rhoasClient.createTopicAndGrantAccess(TEST_TOPIC_INPUT, TEST_SERVICE_ACCOUNT_ID, accessType)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        if (expectFalure) {
            subscriber.awaitFailure(Duration.ofSeconds(TIMEOUT_SECONDS));
        } else {
            subscriber.awaitItem(Duration.ofSeconds(TIMEOUT_SECONDS));
        }

        verifyWireMockServer(expectedPostTopics, expectedPostACLs, expectedDeleteTopics, expectedDeleteACLs);
    }

    private void testDeleteTopicAndRevokeAccess(RhoasTopicAccessType accessType, boolean expectFalure, int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        UniAssertSubscriber<?> subscriber = rhoasClient.deleteTopicAndRevokeAccess(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_ID, accessType)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        if (expectFalure) {
            subscriber.awaitFailure(Duration.ofSeconds(TIMEOUT_SECONDS));
        } else {
            subscriber.awaitItem(Duration.ofSeconds(TIMEOUT_SECONDS));
        }

        verifyWireMockServer(expectedPostTopics, expectedPostACLs, expectedDeleteTopics, expectedDeleteACLs);
    }

    private void verifyWireMockServer(int expectedPostTopics, int expectedPostACLs, int expectedDeleteTopics, int expectedDeleteACLs) {
        wireMockServer.verify(expectedPostTopics, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(expectedPostACLs, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(expectedDeleteTopics, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(expectedDeleteACLs, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

}
