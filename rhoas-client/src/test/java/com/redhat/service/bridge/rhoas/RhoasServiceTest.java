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

import static com.redhat.service.bridge.rhoas.resourcemanager.KafkaInstanceAdminMockServerConfigurator.TEST_TOPIC_NAME;
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
        kafkaMgmtConfigurator.configureWithEmptyTestServiceAccount(wireMockServer);
        kafkaInstanceConfigurator.configureWithEmptyTestTopic(wireMockServer);

        rhoasService.createTopicAndConsumerServiceAccount(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(5));
    }

    @Test
    void testCreateTopicAndConsumerServiceAccountExistingTopic() {
        kafkaMgmtConfigurator.configureWithEmptyTestServiceAccount(wireMockServer);
        kafkaInstanceConfigurator.configureWithExistingTestTopic(wireMockServer);

        rhoasService.createTopicAndConsumerServiceAccount(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5));
    }
}
