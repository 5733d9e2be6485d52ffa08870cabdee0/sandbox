package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountRequest;
import com.redhat.service.bridge.test.rhoas.RhoasMockServerResource;
import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

import static com.redhat.service.bridge.test.rhoas.KafkaInstanceAdminMockServerConfigurator.TEST_TOPIC_NAME;
import static com.redhat.service.bridge.test.rhoas.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_ID;
import static com.redhat.service.bridge.test.rhoas.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_NAME;

@QuarkusTest
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
@TestProfile(RhoasEnabledTestProfile.class)
class RhoasClientTest extends RhoasTestBase {

    @Inject
    Instance<RhoasClient> rhoasClient;

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountOk() {
        configureMockAPIWithAllWorking();

        rhoasClient.get().createTopicAndConsumerServiceAccount(new TopicAndServiceAccountRequest(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(60));

        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(3, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();

        rhoasClient.get().createTopicAndConsumerServiceAccount(new TopicAndServiceAccountRequest(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(60));

        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();

        rhoasClient.get().createTopicAndConsumerServiceAccount(new TopicAndServiceAccountRequest(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(60));

        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();

        rhoasClient.get().createTopicAndConsumerServiceAccount(new TopicAndServiceAccountRequest(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(60));

        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(3, WireMock.postRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(3, WireMock.deleteRequestedFor(WireMock.urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

}
