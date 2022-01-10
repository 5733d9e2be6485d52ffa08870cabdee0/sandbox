package com.redhat.service.bridge.rhoas;

import javax.inject.Inject;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.redhat.service.bridge.test.rhoas.KafkaInstanceAdminMockServerConfigurator;
import com.redhat.service.bridge.test.rhoas.KafkaMgmtV1MockServerConfigurator;
import com.redhat.service.bridge.test.rhoas.MasSSOMockServerConfigurator;
import com.redhat.service.bridge.test.rhoas.RedHatSSOMockServerConfiguration;
import com.redhat.service.bridge.test.wiremock.InjectWireMock;

abstract class RhoasTestBase {

    @InjectWireMock
    protected WireMockServer wireMockServer;

    @Inject
    protected RedHatSSOMockServerConfiguration redHatSSOConfigurator;
    @Inject
    protected MasSSOMockServerConfigurator masSSOConfigurator;
    @Inject
    protected KafkaMgmtV1MockServerConfigurator kafkaMgmtConfigurator;
    @Inject
    protected KafkaInstanceAdminMockServerConfigurator kafkaInstanceConfigurator;

    protected void configureMockSSO() {
        redHatSSOConfigurator.configure(wireMockServer);
        masSSOConfigurator.configure(wireMockServer);
    }

    protected void configureMockAPIWithAllWorking() {
        kafkaMgmtConfigurator.configureWithAllWorking(wireMockServer);
        kafkaInstanceConfigurator.configureWithAllWorking(wireMockServer);
    }

    protected void configureMockAPIWithBrokenTopicCreation() {
        kafkaMgmtConfigurator.configureWithAllWorking(wireMockServer);
        kafkaInstanceConfigurator.configureWithBrokenTopicCreation(wireMockServer);
    }

    protected void configureMockAPIWithBrokenServiceAccountCreation() {
        kafkaMgmtConfigurator.configureWithBrokenServiceAccountCreation(wireMockServer);
        kafkaInstanceConfigurator.configureWithAllWorking(wireMockServer);
    }

    protected void configureMockAPIWithBrokenACLCreation() {
        kafkaMgmtConfigurator.configureWithAllWorking(wireMockServer);
        kafkaInstanceConfigurator.configureWithBrokenACLCreation(wireMockServer);
    }
}
