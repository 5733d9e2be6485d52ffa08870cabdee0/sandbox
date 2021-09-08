package com.redhat.developer.manager.api.user;

import javax.inject.Inject;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.manager.CustomerIdResolver;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;
import com.redhat.developer.manager.api.models.responses.ProcessorResponse;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.api.models.responses.BridgeListResponse;
import com.redhat.developer.manager.api.models.responses.BridgeResponse;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;
import com.redhat.developer.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    CustomerIdResolver customerIdResolver;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        Assertions.assertEquals(0, response.getItems().size());
    }

    @Test
    public void createBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
    }

    @Test
    public void getBridge() {
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        bridgeCreateResponse.then().statusCode(201);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        Assertions.assertNotNull(retrievedBridge);
        Assertions.assertEquals(bridge.getId(), retrievedBridge.getId());
        Assertions.assertEquals(bridge.getName(), retrievedBridge.getName());
        Assertions.assertEquals(bridge.getEndpoint(), retrievedBridge.getEndpoint());
    }

    @Test
    public void getUnexistingBridge() {
        TestUtils.getBridge("not-the-id").then().statusCode(404);
    }

    @Test
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        Assertions.assertEquals(1, bridgeListResponse.getItems().size());
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_BRIDGE_NAME, bridgeResponse.getName());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridgeResponse.getStatus());
        Assertions.assertEquals("/api/v1/bridges/" + bridgeResponse.getId(), bridgeResponse.getHref());
        Assertions.assertNotNull(bridgeResponse.getSubmittedAt());

        Assertions.assertNull(bridgeResponse.getEndpoint());
    }

    @Test
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }

    @Test
    public void addProcessorToBridge() {

        BridgeRequest r = new BridgeRequest("myBridge");
        BridgeResponse bridgeResponse = TestUtils.createBridge(r).as(BridgeResponse.class);

        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridgeResponse.getId());
        dto.setStatus(BridgeStatus.AVAILABLE);
        dto.setCustomerId(customerIdResolver.resolveCustomerId());
        dto.setEndpoint("https://foo.bridges.redhat.com");

        Response deployment = TestUtils.updateBridge(dto);
        assertThat(deployment.getStatusCode(), equalTo(200));

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName(), equalTo("myProcessor"));
        assertThat(processorResponse.getBridge().getId(), equalTo(bridgeResponse.getId()));
    }

    @Test
    public void addProcessorToBridge_bridgeDoesNotExist() {

        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(404));
    }

    @Test
    public void addProcessorToBridge_bridgeNotInAvailableStatus() {

        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest("myBridge")).as(BridgeResponse.class);
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        Response response = RestAssured.given().contentType(ContentType.JSON).when().body(new ProcessorRequest()).post("/api/v1/bridges/myBridge/processors");
        assertThat(response.getStatusCode(), equalTo(400));
    }
}
