package com.redhat.service.bridge.rhoas.resourcemanager;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.redhat.service.bridge.rhoas.dto.ServiceAccount;

import static com.redhat.service.bridge.rhoas.resourcemanager.RedHatSSOMockServerConfiguration.TEST_ACCESS_TOKEN;

@ApplicationScoped
public class KafkaMgmtV1MockServerConfigurator extends AbstractApiMockServerConfigurator {

    public static final String TEST_SERVICE_ACCOUNT_ID = "sa-123";
    public static final String TEST_SERVICE_ACCOUNT_NAME = "test-sa";

    @ConfigProperty(name = "mock-server.mgmt-api.base-path")
    String basePath;

    public KafkaMgmtV1MockServerConfigurator() {
        super("/api/kafkas_mgmt/v1", TEST_ACCESS_TOKEN);
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }

    public void configureWithAllWorking(WireMockServer server) {
        server.stubFor(authPost("/service_accounts").willReturn(responseWithCreatedServiceAccount()));
        server.stubFor(authDelete("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID).willReturn(responseWithStatus(204)));
    }

    public void configureWithBrokenServiceAccountCreation(WireMockServer server) {
        server.stubFor(authPost("/service_accounts").willReturn(responseWithStatus(500)));
        server.stubFor(authDelete("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID).willReturn(responseWithStatus(204)));
    }

    private ResponseDefinitionBuilder responseWithCreatedServiceAccount() {
        ServiceAccount body = new ServiceAccount();
        body.setId(TEST_SERVICE_ACCOUNT_ID);
        body.setName(TEST_SERVICE_ACCOUNT_NAME);
        body.setClientId("srvc-acct-ABC");
        body.setOwner("test_user");
        return jsonResponse(body);
    }
}
