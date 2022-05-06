package com.redhat.service.smartevents.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.context.BridgeContext;
import com.redhat.service.smartevents.integration.tests.context.ProcessorContext;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.ProcessorResource;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;

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

    @And("^the list of Processor instances of the Bridge \"([^\"]*)\" is containing the Processor \"([^\"]*)\"$")
    public void listOfProcessorInstancesOfBridgeIsContainingProcessor(String testBridgeName, String processorName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        ProcessorContext processorContext = bridgeContext.getProcessor(processorName);

        ProcessorListResponse response = ProcessorResource.getProcessorList(context.getManagerToken(),
                bridgeContext.getId());

        assertThat(response.getItems()).anyMatch(p -> p.getId().equals(processorContext.getId()));
    }

    @And("^the list of Processor instances of the Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void listOfProcessorInstancesOfBridgeIsFailingWithHTTPResponseCode(String testBridgeName, int responseCode) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        ProcessorResource.getProcessorListResponse(context.getManagerToken(), bridgeContext.getId())
                .then()
                .statusCode(responseCode);
    }

    @When("^add a Processor to the Bridge \"([^\"]*)\" with body:$")
    public void addProcessorToBridgeWithBody(String testBridgeName, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        processorRequestJson = ContextResolver.resolveWithScenarioContext(context, processorRequestJson);

        JsonObject json = new JsonObject(processorRequestJson);
        String processorName = json.getString("name");

        ProcessorResponse response;
        try (InputStream resourceStream = new ByteArrayInputStream(
                processorRequestJson.getBytes(StandardCharsets.UTF_8))) {
            response = ProcessorResource.createProcessor(context.getManagerToken(),
                    bridgeContext.getId(), resourceStream);
        } catch (IOException e) {
            throw new RuntimeException("Error with inputstream", e);
        }

        bridgeContext.newProcessor(processorName, response.getId());

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
    }

    @When("^add a fake Processor \"([^\"]*)\" to the Bridge \"([^\"]*)\"$")
    public void addFakeProcessorToBridge(String processorName, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        ProcessorContext processorContext = bridgeContext.newProcessor(processorName, UUID.randomUUID().toString());
        processorContext.setDeleted(true);
    }

    @Then("add a Processor to the Bridge \"([^\"]*)\" with body is failing with HTTP response code (\\d+):$")
    public void addProcessorToBridgeWithBodyIsFailingWithHTTPResponseCode(String testBridgeName,
            int responseCode, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        try (InputStream resourceStream = new ByteArrayInputStream(
                processorRequestJson.getBytes(StandardCharsets.UTF_8))) {
            ProcessorResource
                    .createProcessorResponse(context.getManagerToken(), bridgeContext.getId(), resourceStream)
                    .then()
                    .statusCode(responseCode);
        } catch (IOException e) {
            throw new RuntimeException("Error with inputstream", e);
        }
    }

    @When("^update the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" with body:$")
    public void updateProcessorOfTheBridgeWithBody(String processorName, String testBridgeName, String processorRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        processorRequestJson = ContextResolver.resolveWithScenarioContext(context, processorRequestJson);
        ProcessorContext processorContext = bridgeContext.getProcessor(processorName);

        JsonObject json = new JsonObject(processorRequestJson);
        String newProcessorName = json.getString("name");

        ProcessorResponse response;
        try (InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8))) {
            response = ProcessorResource.updateProcessor(context.getManagerToken(), bridgeContext.getId(), processorContext.getId(), resourceStream);
        } catch (IOException e) {
            throw new RuntimeException("Error opening inputstream", e);
        }

        bridgeContext.removeProcessor(processorName);
        bridgeContext.newProcessor(newProcessorName, response.getId());

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
    }

    @And("^get Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void getProcessorOfBridgeIsFailingWithHTTPResponseCode(String processorName, String testBridgeName,
            int responseCode) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        ProcessorResource.getProcessorResponse(context.getManagerToken(), bridgeContext.getId(), processorId)
                .then()
                .statusCode(responseCode);
    }

    @Then("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is existing with status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void processorOfBridgeIsExistingWithStatusWithinMinutes(String processorName, String testBridgeName,
            String status, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getId(),
                                        processorId)
                                .then().log().all()))
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
        Action action = getProcessorAction(processorName, testBridgeName);
        assertThat(action.getType()).isEqualTo(actionType);
        parametersDatatable.asMap().forEach((key, value) -> {
            String parameterTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, value);
            assertThat(action.getParameters()).containsEntry(key, parameterTextWithoutPlaceholders);
        });
    }

    @And("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" has (action|source) of type \"([^\"]*)\"$")
    public void processorOfBridgeHasActionOfType(String processorName, String testBridgeName,
            String processorType,
            String processorTypeValue) {
        final Gateway gateway;
        if (Objects.equals(processorType, "action")) {
            gateway = getProcessorAction(processorName, testBridgeName);
        } else {
            gateway = getProcessorSource(processorName, testBridgeName);
        }
        assertThat(gateway.getType()).isEqualTo(processorTypeValue);
    }

    @When("^delete the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\"$")
    public void deleteProcessorOfBridge(String processorName, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        ProcessorResource.deleteProcessor(context.getManagerToken(), bridgeContext.getId(), processorId);
        bridgeContext.removeProcessor(processorName);
    }

    @When("^delete the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void deleteProcessorOfBridgeIsFailingWithHTTPResponseCode(String processorName, String testBridgeName,
            int responseCode) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        ProcessorResource.deleteProcessorResponse(context.getManagerToken(), bridgeContext.getId(), processorId)
                .then()
                .statusCode(responseCode);
    }

    @Then("^the Processor \"([^\"]*)\" of the Bridge \"([^\"]*)\" is not existing within (\\d+) (?:minute|minutes)$")
    public void processorOfBridgeIsNotExistingWithinMinutes(String processorName, String testBridgeName,
            int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getId(),
                                        processorId)
                                .then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), bridgeContext.getId(),
                                        processorId)
                                .then()
                                .statusCode(404));
    }

    public Action getProcessorAction(String processorName, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        return ProcessorResource.getProcessor(context.getManagerToken(),
                bridgeContext.getId(), processorId).getAction();
    }

    private Source getProcessorSource(String processorName, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String processorId = bridgeContext.getProcessor(processorName).getId();

        return ProcessorResource.getProcessor(context.getManagerToken(),
                bridgeContext.getId(), processorId).getSource();
    }
}
