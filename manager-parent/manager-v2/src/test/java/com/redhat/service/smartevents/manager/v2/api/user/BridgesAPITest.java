package com.redhat.service.smartevents.manager.v2.api.user;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorDAO;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeErrorDAO errorDAO;

    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge() {
        Response response = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));

        response.then().statusCode(202);
        BridgeResponse bridgeResponse = response.as(BridgeResponse.class);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getId()).isNotNull();
        assertThat(bridgeResponse.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getPublishedAt()).isNull();
        assertThat(bridgeResponse.getModifiedAt()).isNotNull();
        assertThat(bridgeResponse.getHref()).contains(bridgeResponse.getId());
        assertThat(bridgeResponse.getOwner()).isNotNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
        assertThat(bridgeResponse.getStatusMessage()).isNull();
    }

    @Test
    public void createBridgeNoAuthentication() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridgeOrganisationWithNoQuota() {
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("organisation-with-no-quota");
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(402).body("kind", Matchers.equalTo("Errors"));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createInvalidBridge() {
        TestUtils.createBridge(new BridgeRequest())
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge_withInvalidCloudProvider() {
        ErrorsResponse errorsResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, "dodgyCloudProvider", DEFAULT_REGION))
                .as(ErrorsResponse.class);

        assertErrorResponses(errorsResponse, Set.of(InvalidCloudProviderException.class, InvalidRegionException.class));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge_withInvalidRegion() {
        ErrorsResponse errorsResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, "dodgyRegion"))
                .as(ErrorsResponse.class);

        assertErrorResponses(errorsResponse, Set.of(InvalidRegionException.class));
    }

    private void assertErrorResponses(ErrorsResponse errorsResponse, Set<Class<? extends RuntimeException>> exceptions) {
        Set<String> expectedErrorCodes = exceptions.stream().map(e -> errorDAO.findByException(e).getCode()).collect(Collectors.toSet());

        assertThat(errorsResponse.getItems().stream().allMatch(x -> x.getHref().contains(V2APIConstants.V2_ERROR_API_BASE_PATH))).isTrue();
        assertThat(errorsResponse.getItems())
                .hasSize(expectedErrorCodes.size())
                .map(ErrorResponse::getCode)
                .allSatisfy(expectedErrorCodes::contains);
    }
}
