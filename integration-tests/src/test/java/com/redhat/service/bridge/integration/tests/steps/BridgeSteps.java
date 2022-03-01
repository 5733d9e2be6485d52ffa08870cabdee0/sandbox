package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.common.Utils;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;

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

    @Given("get list of Bridge instances fails with HTTP response code (\\d+)$")
    public void authenticationIsEnabled(int responseCode) {
        BridgeResource.getBridgeListResponse(context.getManagerToken())
                .then()
                .statusCode(responseCode);
    }

    @When("create a Bridge with randomly generated name with access token")
    public void createRandomBridge() {
        String bridgeName = Utils.generateId("bridge");
        int creationRetry = 1;
        while (creationRetry <= BRIDGE_NAME_CREATE_RETRY && isBridgeExisting(bridgeName)) {
            creationRetry++;
            bridgeName = Utils.generateId("bridge");
        }
        if (isBridgeExisting(bridgeName)) {
            throw new RuntimeException("Cannot create a initiate a random bridge name correctly. Please cleanup the environment...");
        }

        BridgeResponse response = BridgeResource.addBridge(context.getManagerToken(),
                bridgeName);
        assertThat(response.getName()).isEqualTo(bridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        context.setRandomBridgeName(bridgeName);
        context.setBridgeId(response.getId());
    }

    @Then("get list of Bridge instances with access token contains Bridge with randomly generated name")
    public void testRandomBridgeExists() {
        BridgeListResponse response = BridgeResource.getBridgeList(context.getManagerToken());
        assertThat(response.getItems()).anyMatch(b -> b.getName().equals(context.getRandomBridgeName()));
    }

    @Then("^get Bridge with access token exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void bridgeDeployedWithinMinutes(String status, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), context.getBridgeId())
                                .then()
                                .body("status", Matchers.equalTo(status))
                                .body("endpoint", Matchers.containsString(context.getBridgeId())));

        BridgeUtils.getOrRetrieveBridgeEndpoint(context);
    }

    @When("delete a Bridge")
    public void testDeleteBridge() {
        BridgeResource.deleteBridge(context.getManagerToken(), context.getBridgeId());
    }

    @Then("^the Bridge doesn't exists within (\\d+) (?:minute|minutes)$")
    public void bridgeDoesNotExistWithinMinutes(int timeoutMinutes) {

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), context.getBridgeId())
                                .then()
                                .statusCode(404));
    }

    private boolean isBridgeExisting(String bridgeName) {
        return BridgeResource.getBridgeList(context.getManagerToken()).getItems().stream()
                .anyMatch(b -> b.getName().equals(bridgeName));
    }
}
