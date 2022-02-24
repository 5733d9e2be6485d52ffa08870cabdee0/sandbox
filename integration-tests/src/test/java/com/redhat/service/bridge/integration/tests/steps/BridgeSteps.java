package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.common.BridgeCommon;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class BridgeSteps {

    @Given("get list of Bridge instances returns HTTP response code (\\d+)$")
    public void authenticationIsEnabled(int responseCode) {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(responseCode);
    }

    @Given("get list of Bridge instances with access token doesn't contain randomly generated Bridge")
    public void generateRandomBridgeName() {
        StepsContext.randomBridgeName = "bridge-" + UUID.randomUUID().toString().substring(0, 4);
        BridgeListResponse response = BridgeCommon.getBridgeList();
        assertThat(response.getKind()).isEqualTo("BridgeList");
        assertThat(response.getItems()).noneMatch(b -> b.getName().equals(StepsContext.randomBridgeName));
    }

    @When("create a Bridge with randomly generated name with access token")
    public void createRandomBridge() {
        BridgeResponse response = BridgeCommon.addBridge(StepsContext.randomBridgeName);
        assertThat(response.getName()).isEqualTo(StepsContext.randomBridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        StepsContext.bridgeId = response.getId();
    }

    @Then("get list of Bridge instances with access token contains Bridge with randomly generated name")
    public void testRandomBridgeExists() {
        BridgeListResponse response = BridgeCommon.getBridgeList();
        assertThat(response.getItems()).anyMatch(b -> b.getName().equals(StepsContext.randomBridgeName));
    }

    @Then("^get Bridge with access token exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void bridgeDeployedWithinMinutes(String status, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeCommon.getBridgeDetails(StepsContext.bridgeId)
                                .then()
                                .body("status", Matchers.equalTo(status))
                                .body("endpoint", Matchers.containsString(StepsContext.bridgeId)));

        // store bridge endpoint details
        StepsContext.endPoint = BridgeCommon.getBridgeDetails(StepsContext.bridgeId).then().extract().response()
                .as(BridgeResponse.class).getEndpoint();
        // If an endpoint contains localhost without port then default port has to be
        // defined, otherwise rest-assured will use port 8080
        if (StepsContext.endPoint.matches("http://localhost/.*")) {
            StepsContext.endPoint = StepsContext.endPoint.replace("http://localhost/", "http://localhost:80/");
        }
    }

    @When("delete a Bridge")
    public void testDeleteBridge() {
        BridgeCommon.deleteBridge(StepsContext.bridgeId);
    }

    @Then("^the Bridge doesn't exists within (\\d+) (?:minute|minutes)$")
    public void bridgeDoesNotExistWithinMinutes(int timeoutMinutes) {

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeCommon.getBridgeDetails(StepsContext.bridgeId)
                                .then()
                                .statusCode(404));
    }

}
