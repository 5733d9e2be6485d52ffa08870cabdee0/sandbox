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

    @When("^a new Processor is added to the Bridge \"([^\"]*)\" with body:$")
    public void newProcesorIsAddedToBridgeWithBody(String testBridgeName, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        JsonObject json = new JsonObject(processorRequestJson);
        String processorName = json.getString("name");
        int filtersSize = json.getJsonArray("filters").size();

        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        ProcessorResponse response = ProcessorResource.createProcessor(context.getManagerToken(),
                bridgeContext.getBridgeId(), resourceStream);

        bridgeContext.newProcessor(processorName, response.getId());

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getFilters().size()).isEqualTo(filtersSize);
    }

    @Then("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" exists with status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void processorOfBridgeExistsWithStatusWithinMinutes(String processorName, String testBridgeName,
            String status, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessorId(processorName);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getBridgeId(),
                                        processorId)
                                .then()
                                .body("status", Matchers.equalTo(status)));
    }

    @And("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" has action of type \"([^\"]*)\" and parameters:$")
    public void processorOfBridgeHasActionOfTypeAndParameters(String processorName, String testBridgeName,
            String actionType, DataTable parametersDatatable) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessorId(processorName);

        ProcessorResponse response = ProcessorResource.getProcessor(context.getManagerToken(),
                bridgeContext.getBridgeId(), processorId);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(actionType);
        parametersDatatable.asMap().forEach((key, value) -> {
            assertThat(action.getParameters()).containsEntry(key, value);
        });
    }

    @When("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is deleted$")
    public void processorOfBridgeIsDeleted(String processorName, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessorId(processorName);

        ProcessorResource.deleteProcessor(context.getManagerToken(), bridgeContext.getBridgeId(), processorId);
        bridgeContext.removeProcessor(processorName);
    }

    @Then("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" does not exists within (\\d+) (?:minute|minutes)$")
    public void processorOfBridgeDoesNotExistsWithinMinutes(String processorName, String testBridgeName,
            int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessorId(processorName, true);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getBridgeId(),
                                        processorId)
                                .then()
                                .statusCode(404));
    }

    @Then("a new Processor is added to the Bridge \"([^\"]*)\" and returns HTTP response code (\\d+) with body:$")
    public void newProcessorIsAddedToBridgeAndReturnsHTTPResponseCodeWithBody(String testBridgeName, int responseCode, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));

        ProcessorResource
                .createProcessorResponse(context.getManagerToken(), bridgeContext.getBridgeId(), resourceStream)
                .then()
                .statusCode(responseCode);
    }
}
