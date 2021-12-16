package com.redhat.service.bridge.rhoas.resourcemanager;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.redhat.service.bridge.rhoas.dto.ServiceAccount;
import com.redhat.service.bridge.rhoas.dto.ServiceAccounts;

import static com.redhat.service.bridge.rhoas.resourcemanager.RedHatSSOMockServerConfiguration.TEST_ACCESS_TOKEN;

@ApplicationScoped
public class KafkaMgmtV1MockServerConfigurator extends AbstractApiMockServerConfigurator {

    public static final String TEST_SERVICE_ACCOUNT_NAME = "test-sa";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ConfigProperty(name = "mock-server.mgmt-api.base-path")
    String basePath;

    public KafkaMgmtV1MockServerConfigurator() {
        super("/api/kafkas_mgmt/v1", TEST_ACCESS_TOKEN);
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }

    public void configureWithEmptyTestServiceAccount(WireMockServer server) {
        server.stubFor(authGet("/service_accounts").willReturn(responseWithEmptyServiceAccounts()));
        server.stubFor(authPost("/service_accounts").willReturn(responseWithCreatedServiceAccount()));
    }

    public void configureWithExistingTestServiceAccount(WireMockServer server) {
        server.stubFor(authGet("/service_accounts").willReturn(responseWithExistingServiceAccount()));
        server.stubFor(authPost("/service_accounts").willReturn(emptyResponse(409)));
    }

    private ResponseDefinitionBuilder responseWithEmptyServiceAccounts() {
        ServiceAccounts body = new ServiceAccounts();
        body.setPage(1);
        body.setSize(0);
        body.setTotal(0);
        body.setItems(Collections.emptyList());
        return jsonResponse(body);
    }

    private ResponseDefinitionBuilder responseWithExistingServiceAccount() {
        ServiceAccount testSA = new ServiceAccount();
        testSA.setId("123");
        testSA.setName(TEST_SERVICE_ACCOUNT_NAME);
        testSA.setClientId("srvc-acct-ABC");
        testSA.setOwner("test_user");

        ServiceAccounts body = new ServiceAccounts();
        body.setPage(1);
        body.setSize(1);
        body.setTotal(0);
        body.setItems(Collections.singletonList(testSA));

        return jsonResponse(body);
    }

    private ResponseDefinitionBuilder responseWithCreatedServiceAccount() {
        ServiceAccount body = new ServiceAccount();
        body.setId("123");
        body.setName(TEST_SERVICE_ACCOUNT_NAME);
        body.setClientId("srvc-acct-ABC");
        body.setOwner("test_user");
        return jsonResponse(body);
    }

}
