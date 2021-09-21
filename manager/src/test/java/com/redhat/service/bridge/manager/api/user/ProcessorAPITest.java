package com.redhat.service.bridge.manager.api.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.actions.ActionRequest;
import com.redhat.service.bridge.infra.models.actions.KafkaTopicAction;
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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
        assertThat(deployment.getStatusCode(), equalTo(200));
        return bridgeResponse;
    }

    private ActionRequest createKafkaAction() {
        ActionRequest a = new ActionRequest();
        a.setName(TestConstants.DEFAULT_ACTION_NAME);
        a.setType(KafkaTopicAction.KAFKA_ACTION_TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.KAFKA_ACTION_TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
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
        assertThat(listResponse.getPage(), equalTo(0L));
        assertThat(listResponse.getSize(), equalTo(2L));
        assertThat(listResponse.getTotal(), equalTo(2L));

        listResponse.getItems().forEach((i) -> assertThat(i.getId(), in(asList(p.getId(), p2.getId()))));
    }

    @Test
    public void listProcessors_pageOffset() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 1, 1).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage(), equalTo(1L));
        assertThat(listResponse.getSize(), equalTo(1L));
        assertThat(listResponse.getTotal(), equalTo(2L));

        assertThat(listResponse.getItems().get(0).getId(), equalTo(p2.getId()));
        assertRequestedAction(listResponse.getItems().get(0));
    }

    @Test
    public void listProcessors_bridgeDoesNotExist() {
        assertThat(TestUtils.listProcessors("doesNotExist", 0, 100).getStatusCode(), equalTo(404));
    }

    @Test
    public void getProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse pr = response.as(ProcessorResponse.class);
        ProcessorResponse found = TestUtils.getProcessor(bridgeResponse.getId(), pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId(), equalTo(pr.getId()));
        assertThat(found.getBridge().getId(), equalTo(bridgeResponse.getId()));
    }

    private void assertRequestedAction(ProcessorResponse processorResponse) {
        ActionRequest actionRequest = processorResponse.getAction();
        assertThat(actionRequest, is(notNullValue()));
        assertThat(actionRequest.getType(), equalTo(KafkaTopicAction.KAFKA_ACTION_TYPE));
        assertThat(actionRequest.getParameters().get(KafkaTopicAction.KAFKA_ACTION_TOPIC_PARAM), equalTo(TestConstants.DEFAULT_KAFKA_TOPIC));
    }

    @Test
    public void getProcessor_processorDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(201));

        Response found = TestUtils.getProcessor(bridgeResponse.getId(), "doesNotExist");
        assertThat(found.getStatusCode(), equalTo(404));
    }

    @Test
    public void getProcessor_bridgeDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);

        Response found = TestUtils.getProcessor("doesNotExist", response.getId());
        assertThat(found.getStatusCode(), equalTo(404));
    }

    @Test
    public void addProcessorToBridge() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName(), equalTo("myProcessor"));
        assertThat(processorResponse.getBridge().getId(), equalTo(bridgeResponse.getId()));
        assertThat(processorResponse.getFilters().size(), equalTo(1));

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName(), equalTo("myProcessor"));
        assertThat(retrieved.getBridge().getId(), equalTo(bridgeResponse.getId()));
        assertThat(retrieved.getFilters().size(), equalTo(1));
        assertRequestedAction(retrieved);
    }

    @Test
    public void addProcessorToBridge_noActionSpecified() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null));

        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorToBridge_unrecognisedActionType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ActionRequest action = TestUtils.createKafkaAction();
        action.setType("thisDoesNotExist");

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, action));

        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorToBridge_missingActionParameters() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ActionRequest action = TestUtils.createKafkaAction();
        action.getParameters().clear();
        action.getParameters().put("thisIsNotCorrect", "myTopic");

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, action));

        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorWithNullFiltersToBridge() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", null, createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName(), equalTo("myProcessor"));
        assertThat(processorResponse.getBridge().getId(), equalTo(bridgeResponse.getId()));
        assertThat(processorResponse.getFilters(), nullValue());
        assertRequestedAction(processorResponse);
    }

    @Test
    public void addProcessorToBridgeAndRetrieve() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), response.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName(), equalTo("myProcessor"));
        assertThat(retrieved.getBridge().getId(), equalTo(bridgeResponse.getId()));
        assertThat(retrieved.getFilters().size(), equalTo(1));
        assertRequestedAction(retrieved);
    }

    @Test
    public void addProcessorToBridge_bridgeDoesNotExist() {

        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(404));
    }

    @Test
    public void addProcessorToBridge_bridgeNotInAvailableStatus() {

        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode(), equalTo(400));
    }
}
