package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.ProcessorResource;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.json.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessorSteps {

    private TestContext context;

    public ProcessorSteps(TestContext context) {
        this.context = context;
    }

    @When("^add Processor to the Bridge with access token:$")
    public void addProcessor(String processorRequestJson) {

        JsonObject json = new JsonObject(processorRequestJson);
        String processorName = json.getString("name");
        String topic = json.getJsonObject("action").getJsonObject("parameters").getString("topic");
        int filtersSize = json.getJsonArray("filters").size();

        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        ProcessorResponse response = ProcessorResource.createProcessor(context.getManagerToken(),
                context.getBridgeId(), resourceStream);

        context.setProcessorId(response.getId());

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(response.getFilters().size()).isEqualTo(filtersSize);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(action.getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, topic);
    }

    @Then("add invalid Processor to the Bridge with access token returns HTTP response code (\\d+):$")
    public void addWrongFilterProcessor(int responseCode, String processorRequestJson) {
        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        ProcessorResource
                .createProcessorResponse(context.getManagerToken(), context.getBridgeId(), resourceStream)
                .then()
                .statusCode(responseCode);
    }

    @Then("^get Processor with access token exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void processorExistsWithinMinutes(String status, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), context.getBridgeId(),
                                        context.getProcessorId())
                                .then()
                                .body("status", Matchers.equalTo(status)));
    }

    @When("the Processor is deleted")
    public void testDeleteProcessor() {
        ProcessorResource.deleteProcessor(context.getManagerToken(), context.getBridgeId(),
                context.getProcessorId());
    }

    @Then("^the Processor doesn't exists within (\\d+) (?:minute|minutes)$")
    public void processorDoesNotExistsWithinMinutes(int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> ProcessorResource
                                .getProcessorResponse(context.getManagerToken(), context.getBridgeId(),
                                        context.getProcessorId())
                                .then()
                                .statusCode(404));
    }
}
