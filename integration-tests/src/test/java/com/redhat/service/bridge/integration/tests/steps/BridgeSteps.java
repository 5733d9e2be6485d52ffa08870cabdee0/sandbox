package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.common.Utils;
import com.redhat.service.bridge.integration.tests.context.BridgeContext;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class BridgeSteps {

    private static final int BRIDGE_NAME_CREATE_RETRY = 20;

    private TestContext context;

    public BridgeSteps(TestContext context) {
        this.context = context;
    }

    @Given("^the list of Bridge instances is failing with HTTP response code (\\d+)$")
    public void listOfBridgeInstancesIsFailingWithHTTPResponseCode(int responseCode) {
        BridgeResource.getBridgeListResponse(context.getManagerToken())
                .then()
                .statusCode(responseCode);
    }

    @When("^create a new Bridge \"([^\"]*)\"$")
    public void createNewBridge(String testBridgeName) {
        String systemBridgeName = Utils.generateId("test-" + testBridgeName);
        int creationRetry = 1;
        while (creationRetry <= BRIDGE_NAME_CREATE_RETRY && isBridgeExisting(systemBridgeName)) {
            creationRetry++;
            systemBridgeName = Utils.generateId("test-" + testBridgeName);
        }
        if (isBridgeExisting(systemBridgeName)) {
            throw new RuntimeException(
                    "Cannot create a initiate a random bridge name correctly. Please cleanup the environment...");
        }

        BridgeResponse response = BridgeResource.addBridge(context.getManagerToken(), systemBridgeName);
        assertThat(response.getName()).isEqualTo(systemBridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        context.newBridge(testBridgeName, response.getId(), systemBridgeName);
    }

    @Given("^create a new Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void createNewBridgeIsFailingWithHTTPResponseCode(String testBridgeName, int responseCode) {
        BridgeResource.addBridgeResponse(context.getManagerToken(), testBridgeName)
                .then()
                .statusCode(responseCode);
    }

    @And("^the list of Bridge instances is containing the Bridge \"([^\"]*)\"$")
    public void listOfBridgeInstancesIsContainingBridge(String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        BridgeListResponse response = BridgeResource.getBridgeList(context.getManagerToken());

        assertThat(response.getItems()).anyMatch(b -> b.getId().equals(bridgeContext.getId()));
        BridgeResponse bridge = response.getItems().stream().filter(b -> b.getId().equals(bridgeContext.getId()))
                .findFirst().orElseThrow();
        assertThat(bridge.getName()).isEqualTo(bridgeContext.getName());
    }

    @And("^get Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void getBridgeIsFailingWithHTTPResponseCode(String testBridgeName, int responseCode) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        BridgeResource.getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                .then()
                .statusCode(responseCode);
    }

    @And("^the Bridge \"([^\"]*)\" is existing with status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void bridgeIsExistingWithStatusWithinMinutes(String testBridgeName, String status, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                                .then()
                                .body("status", Matchers.equalTo(status))
                                .body("endpoint", Matchers.containsString(bridgeContext.getId())));

        BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);
    }

    @When("^delete the Bridge \"([^\"]*)\"$")
    public void deleteBridge(String testBridgeName) {
        BridgeResource.deleteBridge(context.getManagerToken(), context.getBridge(testBridgeName).getId());
        context.removeBridge(testBridgeName);
    }

    @Then("^the Bridge \"([^\"]*)\" is not existing within (\\d+) (?:minute|minutes)$")
    public void bridgeIsNotExistingWithinMinutes(String testBridgeName, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                                .then()
                                .statusCode(404));
    }

    private boolean isBridgeExisting(String bridgeName) {
        return BridgeResource.getBridgeList(context.getManagerToken()).getItems().stream()
                .anyMatch(b -> b.getName().equals(bridgeName));
    }
}
