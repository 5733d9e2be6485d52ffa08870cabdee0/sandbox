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

    private TestContext context;

    public BridgeSteps(TestContext context) {
        this.context = context;
    }

    @Given("^the list of Bridge instances returns HTTP response code (\\d+)$")
    public void listOfBridgeInstancesReturnsHTTPResponseCode(int responseCode) {
        BridgeResource.getBridgeListFails(responseCode);
    }

    @When("^a new Bridge \"([^\"]*)\" is created$")
    public void newBridgeIsCreated(String testBridgeName) {
        String systemBridgeName = Utils.generateId("bridge");
        while (isBridgeExisting(systemBridgeName)) {
            systemBridgeName = Utils.generateId("bridge");
        }

        BridgeResponse response = BridgeResource.addBridge(context.getManagerToken(), systemBridgeName);
        assertThat(response.getName()).isEqualTo(systemBridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        BridgeContext bridgeContext = context.newBridge(testBridgeName, systemBridgeName);
        bridgeContext.setBridgeId(response.getId());
        bridgeContext.setBridgeName(response.getName());
    }

    @And("^the list of Bridge instances contains the Bridge \"([^\"]*)\"+$")
    public void listOfBridgeInstancesContainsBridge(String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        BridgeListResponse response = BridgeResource.getBridgeList(context.getManagerToken());

        assertThat(response.getItems()).anyMatch(b -> b.getId().equals(bridgeContext.getBridgeId()));
        BridgeResponse bridge = response.getItems().stream().filter(b -> b.getId().equals(bridgeContext.getBridgeId()))
                .findFirst().orElseThrow();
        assertThat(bridge.getName()).isEqualTo(bridgeContext.getBridgeName());
    }

    @And("^the Bridge \"([^\"]*)\" exists with status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void bridgeExistsWithStatusWithinMinutes(String testBridgeName, String status, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getBridgeId())
                                .then()
                                .body("status", Matchers.equalTo(status))
                                .body("endpoint", Matchers.containsString(bridgeContext.getBridgeId())));

        BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);
    }

    @When("^the Bridge \"([^\"]*)\" is deleted$")
    public void bridgeIsDeleted(String testBridgeName) {
        BridgeResource.deleteBridge(context.getManagerToken(), context.getBridge(testBridgeName).getBridgeId());
        context.removeBridge(testBridgeName);
    }

    @Then("^the Bridge \"([^\"]*)\" does not exist within (\\d+) (?:minute|minutes)$")
    public void bridgeDoesNotExistWithinMinutes(String testBridgeName, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName, true);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getBridgeId())
                                .then()
                                .statusCode(404));
    }

    private boolean isBridgeExisting(String bridgeName) {
        return BridgeResource.getBridgeList(context.getManagerToken()).getItems().stream()
                .anyMatch(b -> b.getName().equals(bridgeName));
    }
}
