package com.redhat.service.bridge.manager.api.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ProcessorAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        BridgeResponse bridgeResponse = TestUtils.createBridge(r).as(BridgeResponse.class);
        return bridgeResponse;
    }

    private BridgeResponse createAndDeployBridge() {
        BridgeResponse bridgeResponse = createBridge();

        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridgeResponse.getId());
        dto.setStatus(BridgeStatus.AVAILABLE);
        dto.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        dto.setEndpoint("https://foo.bridges.redhat.com");

        Response deployment = TestUtils.updateBridge(dto);
        assertThat(deployment.getStatusCode()).isEqualTo(200);
        return bridgeResponse;
    }

    private BaseAction createKafkaAction() {
        BaseAction a = new BaseAction();
        a.setName(TestConstants.DEFAULT_ACTION_NAME);
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);
        return a;
    }

    @BeforeEach
    public void beforeEach() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void listProcessors() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 0, 100).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(2L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        listResponse.getItems().forEach((i) -> assertThat(i.getId()).isIn(p.getId(), p2.getId()));
    }

    @Test
    public void listProcessors_pageOffset() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 1, 1).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isEqualTo(1L);
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertRequestedAction(listResponse.getItems().get(0));
    }

    @Test
    public void listProcessors_bridgeDoesNotExist() {
        assertThat(TestUtils.listProcessors("doesNotExist", 0, 100).getStatusCode()).isEqualTo(404);
    }

    @Test
    public void getProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(201);

        ProcessorResponse pr = response.as(ProcessorResponse.class);
        ProcessorResponse found = TestUtils.getProcessor(bridgeResponse.getId(), pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId()).isEqualTo(pr.getId());
        assertThat(found.getBridge().getId()).isEqualTo(bridgeResponse.getId());
    }

    private void assertRequestedAction(ProcessorResponse processorResponse) {
        BaseAction baseAction = processorResponse.getAction();
        assertThat(baseAction).isNotNull();
        assertThat(baseAction.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(baseAction.getParameters().get(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    public void getProcessor_processorDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(201);

        Response found = TestUtils.getProcessor(bridgeResponse.getId(), "doesNotExist");
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    public void getProcessor_bridgeDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);

        Response found = TestUtils.getProcessor("doesNotExist", response.getId());
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    public void addProcessorToBridge() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, "{}", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(201);

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName()).isEqualTo("myProcessor");
        assertThat(processorResponse.getBridge().getId()).isEqualTo(bridgeResponse.getId());
        assertThat(processorResponse.getFilters().size()).isEqualTo(1);

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName()).isEqualTo("myProcessor");
        assertThat(retrieved.getBridge().getId()).isEqualTo(bridgeResponse.getId());
        assertThat(retrieved.getFilters().size()).isEqualTo(1);
        assertThat(retrieved.getTransformationTemplate()).isEqualTo("{}");
        assertRequestedAction(retrieved);
    }

    @Test
    public void addProcessorToBridge_noActionSpecified() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, null));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorToBridge_unrecognisedActionType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        BaseAction action = TestUtils.createKafkaAction();
        action.setType("thisDoesNotExist");

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, action));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorToBridge_missingActionParameters() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        BaseAction action = TestUtils.createKafkaAction();
        action.getParameters().clear();
        action.getParameters().put("thisIsNotCorrect", "myTopic");

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, action));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorWithNullFiltersToBridge() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(201);

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName()).isEqualTo("myProcessor");
        assertThat(processorResponse.getBridge().getId()).isEqualTo(bridgeResponse.getId());
        assertThat(processorResponse.getFilters()).isNull();
        assertThat(processorResponse.getTransformationTemplate()).isNull();
        assertRequestedAction(processorResponse);
    }

    @Test
    public void addProcessorToBridgeAndRetrieve() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(201);

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), response.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName()).isEqualTo("myProcessor");
        assertThat(retrieved.getBridge().getId()).isEqualTo(bridgeResponse.getId());
        assertThat(retrieved.getFilters().size()).isEqualTo(1);
        assertThat(retrieved.getTransformationTemplate()).isNull();
        assertRequestedAction(retrieved);
    }

    @Test
    public void addProcessorToBridge_bridgeDoesNotExist() {

        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    public void addProcessorToBridge_bridgeNotInAvailableStatus() {

        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void testDeleteProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", null, null, createKafkaAction())).as(ProcessorResponse.class);

        TestUtils.deleteProcessor(bridgeResponse.getId(), processorResponse.getId()).then().statusCode(202);
        processorResponse = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);

        assertThat(processorResponse.getStatus()).isEqualTo(BridgeStatus.DELETION_REQUESTED);
    }
}
