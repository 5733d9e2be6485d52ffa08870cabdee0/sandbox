package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.redhat.service.bridge.rhoas.resourcemanager.KafkaInstanceAdminMockServerConfigurator;
import com.redhat.service.bridge.rhoas.resourcemanager.KafkaMgmtV1MockServerConfigurator;
import com.redhat.service.bridge.rhoas.resourcemanager.MasSSOMockServerConfigurator;
import com.redhat.service.bridge.rhoas.resourcemanager.MockServerResource;
import com.redhat.service.bridge.rhoas.resourcemanager.RedHatSSOMockServerConfiguration;
import com.redhat.service.bridge.test.wiremock.InjectWireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.redhat.service.bridge.rhoas.resourcemanager.KafkaInstanceAdminMockServerConfigurator.TEST_TOPIC_NAME;
import static com.redhat.service.bridge.rhoas.resourcemanager.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_ID;
import static com.redhat.service.bridge.rhoas.resourcemanager.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_NAME;

@QuarkusTest
@QuarkusTestResource(value = MockServerResource.class, restrictToAnnotatedClass = true)
class RhoasServiceTest {

    @InjectWireMock
    WireMockServer wireMockServer;

    @Inject
    RedHatSSOMockServerConfiguration redHatSSOConfigurator;
    @Inject
    MasSSOMockServerConfigurator masSSOConfigurator;
    @Inject
    KafkaMgmtV1MockServerConfigurator kafkaMgmtConfigurator;
    @Inject
    KafkaInstanceAdminMockServerConfigurator kafkaInstanceConfigurator;

    @Inject
    RhoasService rhoasService;

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        redHatSSOConfigurator.configure(wireMockServer);
        masSSOConfigurator.configure(wireMockServer);
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountOk() {
        kafkaMgmtConfigurator.configureWithAllWorking(wireMockServer);
        kafkaInstanceConfigurator.configureWithAllWorking(wireMockServer);

        rhoasService.createTopicAndConsumerServiceAccount(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME)
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(5));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(3, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(0, deleteRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(0, deleteRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(0, deleteRequestedFor(urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountWithBrokenTopicCreation() {
        kafkaMgmtConfigurator.configureWithAllWorking(wireMockServer);
        kafkaInstanceConfigurator.configureWithBrokenTopicCreation(wireMockServer);

        rhoasService.createTopicAndConsumerServiceAccount(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME)
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(0, postRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(0, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(0, deleteRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(0, deleteRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(0, deleteRequestedFor(urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountWithBrokenServiceAccountCreation() {
        kafkaMgmtConfigurator.configureWithBrokenServiceAccountCreation(wireMockServer);
        kafkaInstanceConfigurator.configureWithAllWorking(wireMockServer);

        rhoasService.createTopicAndConsumerServiceAccount(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME)
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(0, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(1, deleteRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(0, deleteRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(0, deleteRequestedFor(urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountWithBrokenACLCreation() {
        kafkaMgmtConfigurator.configureWithAllWorking(wireMockServer);
        kafkaInstanceConfigurator.configureWithBrokenACLCreation(wireMockServer);

        rhoasService.createTopicAndConsumerServiceAccount(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME)
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics"))));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts"))));
        wireMockServer.verify(3, postRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/acls"))));

        wireMockServer.verify(1, deleteRequestedFor(urlEqualTo(kafkaInstanceConfigurator.pathOf("/topics/" + TEST_TOPIC_NAME))));
        wireMockServer.verify(1, deleteRequestedFor(urlEqualTo(kafkaMgmtConfigurator.pathOf("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID))));
        wireMockServer.verify(3, deleteRequestedFor(urlMatching(kafkaInstanceConfigurator.pathOf("/acls?.*"))));
    }
}
