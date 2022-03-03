package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.context.BridgeContext;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.ProcessorResource;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.json.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessorSteps {

    private TestContext context;

    public ProcessorSteps(TestContext context) {
        this.context = context;
    }

    @When("^add a Processor to the Bridge \"([^\"]*)\" with body:$")
    public void addProcessorToBridgeWithBody(String testBridgeName, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        JsonObject json = new JsonObject(processorRequestJson);
        String processorName = json.getString("name");
        int filtersSize = json.getJsonArray("filters").size();

        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        ProcessorResponse response = ProcessorResource.createProcessor(context.getManagerToken(),
                bridgeContext.getId(), resourceStream);

        bridgeContext.newProcessor(processorName, response.getId());

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getFilters().size()).isEqualTo(filtersSize);
    }

    @Then("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is existing with status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void processorOfBridgeIsExistingWithStatusWithinMinutes(String processorName, String testBridgeName,
            String status, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getId(),
                                        processorId)
                                .then()
                                .body("status", Matchers.equalTo(status)));
    }

    @And("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" has action of type \"([^\"]*)\" and parameters:$")
    public void processorOfBridgeHasActionOfTypeAndParameters(String processorName, String testBridgeName,
            String actionType, DataTable parametersDatatable) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        ProcessorResponse response = ProcessorResource.getProcessor(context.getManagerToken(),
                bridgeContext.getId(), processorId);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(actionType);
        parametersDatatable.asMap().forEach((key, value) -> {
            assertThat(action.getParameters()).containsEntry(key, value);
        });
    }

    @When("^delete the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\"$")
    public void deleteProcessorOfBridge(String processorName, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        ProcessorResource.deleteProcessor(context.getManagerToken(), bridgeContext.getId(), processorId);
        bridgeContext.removeProcessor(processorName);
    }

    @Then("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is not existing within (\\d+) (?:minute|minutes)$")
    public void processorOfBridgeIsNotExistingWithinMinutes(String processorName, String testBridgeName,
            int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getId(),
                                        processorId)
                                .then()
                                .statusCode(404));
    }

    @Then("add a Processor to the Bridge \"([^\"]*)\" and returns HTTP response code (\\d+) with body:$")
    public void newProcessorIsAddedToBridgeAndReturnsHTTPResponseCodeWithBody(String testBridgeName, int responseCode, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));

        ProcessorResource
                .createProcessorResponse(context.getManagerToken(), bridgeContext.getId(), resourceStream)
                .then()
                .statusCode(responseCode);
    }
}
