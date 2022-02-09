package com.redhat.service.bridge.test.rhoas;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

@ApplicationScoped
public class KafkaMgmtV1MockServerConfigurator extends AbstractApiMockServerConfigurator {

    public static final String TEST_SERVICE_ACCOUNT_ID = "sa-123";
    public static final String TEST_SERVICE_ACCOUNT_NAME = "test-sa";

    @ConfigProperty(name = "rhoas-mock-server.mgmt-api.base-path")
    String basePath;

    public KafkaMgmtV1MockServerConfigurator() {
        super("/api/kafkas_mgmt/v1", RedHatSSOMockServerConfigurator.TEST_ACCESS_TOKEN);
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

    public void configureWithBrokenServiceAccountDeletion(WireMockServer server) {
        server.stubFor(authPost("/service_accounts").willReturn(responseWithCreatedServiceAccount()));
        server.stubFor(authDelete("/service_accounts/" + TEST_SERVICE_ACCOUNT_ID).willReturn(responseWithStatus(500)));
    }

    private ResponseDefinitionBuilder responseWithCreatedServiceAccount() {
        Map<String, String> body = new HashMap<>();
        body.put("id", TEST_SERVICE_ACCOUNT_ID);
        body.put("name", TEST_SERVICE_ACCOUNT_NAME);
        body.put("clientId", "srvc-acct-ABC");
        body.put("owner", "test_user");
        return jsonResponse(body);
    }
}
