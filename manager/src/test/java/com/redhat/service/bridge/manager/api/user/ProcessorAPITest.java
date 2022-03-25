package com.redhat.service.bridge.manager.api.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.filters.ValuesIn;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.WorkerSchedulerProfile;
import com.redhat.service.bridge.manager.actions.connectors.SlackAction;
import com.redhat.service.bridge.manager.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import static com.redhat.service.bridge.manager.utils.TestUtils.createKafkaAction;
import static com.redhat.service.bridge.manager.utils.TestUtils.createSendToBridgeAction;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
public class ProcessorAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    @SuppressWarnings("unused")
    //Although this is unused, we need to inject it to set-up RHOAS
    RhoasService rhoasServiceMock;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    public void testAuthentication() {
        TestUtils.getProcessor(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
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
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors_pageOffset() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 1, 1).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isEqualTo(1L);
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertRequestedAction(listResponse.getItems().get(0));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors_bridgeDoesNotExist() {
        assertThat(TestUtils.listProcessors("doesNotExist", 0, 100).getStatusCode()).isEqualTo(404);
    }

    @Test
    public void listProcessorsNoAuthentication() {
        assertThat(TestUtils.listProcessors("any-id", 0, 100).getStatusCode()).isEqualTo(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse pr = response.as(ProcessorResponse.class);

        assertThat(pr.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(pr.getAction().getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);

        ProcessorResponse found = TestUtils.getProcessor(bridgeResponse.getId(), pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId()).isEqualTo(pr.getId());
        assertThat(found.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(found.getAction().getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessorWithSendToBridgeAction() {
        BridgeResponse bridgeResponse = createAndDeployBridge();
        String bridgeId = bridgeResponse.getId();

        Response response = TestUtils.addProcessorToBridge(bridgeId, new ProcessorRequest("myProcessor", createSendToBridgeAction(bridgeId)));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse pr = response.as(ProcessorResponse.class);

        assertThat(pr.getAction().getType()).isEqualTo(SendToBridgeAction.TYPE);
        assertThat(pr.getAction().getParameters()).containsEntry(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);

        ProcessorResponse found = TestUtils.getProcessor(bridgeId, pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId()).isEqualTo(pr.getId());
        assertThat(found.getAction().getType()).isEqualTo(SendToBridgeAction.TYPE);
        assertThat(found.getAction().getParameters()).containsEntry(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
    }

    private void assertRequestedAction(ProcessorResponse processorResponse) {
        BaseAction baseAction = processorResponse.getAction();
        assertThat(baseAction).isNotNull();
        assertThat(baseAction.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(baseAction.getParameters().get(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor_processorDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        Response found = TestUtils.getProcessor(bridgeResponse.getId(), "doesNotExist");
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor_bridgeDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction())).as(ProcessorResponse.class);

        Response found = TestUtils.getProcessor("doesNotExist", response.getId());
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, "{}", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName()).isEqualTo("myProcessor");
        assertThat(processorResponse.getFilters().size()).isEqualTo(1);

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName()).isEqualTo("myProcessor");
        assertThat(retrieved.getFilters().size()).isEqualTo(1);
        assertThat(retrieved.getTransformationTemplate()).isEqualTo("{}");
        assertRequestedAction(retrieved);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_noActionSpecified() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, null));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_unrecognisedActionType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        BaseAction action = createKafkaAction();
        action.setType("thisDoesNotExist");

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, action));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_missingActionParameters() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        BaseAction action = createKafkaAction();
        action.getParameters().clear();
        action.getParameters().put("thisIsNotCorrect", "myTopic");

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, action));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithNullFiltersToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName()).isEqualTo("myProcessor");
        assertThat(processorResponse.getFilters()).isNull();
        assertThat(processorResponse.getTransformationTemplate()).isNull();
        assertRequestedAction(processorResponse);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithWrongFilterFiltersToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", Collections.singleton(new ValuesIn("pepe", null)), null, createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithEmptyChannelParameterToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        BaseAction action = createKafkaAction();
        action.setType(SlackAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAMETER, "");
        params.put(SlackAction.WEBHOOK_URL_PARAMETER, "https://example.com");
        action.setParameters(params);

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", null, null, action));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithEmptyWebhookURLParameterToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        BaseAction action = createKafkaAction();
        action.setType(SlackAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAMETER, "channel");
        params.put(SlackAction.WEBHOOK_URL_PARAMETER, "");
        action.setParameters(params);

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", null, null, action));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithWrongParametersNameToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        String requestBody = String.format("{\"name\": \"processorInvalid\", \"action\": {\"type\": \"Slack\", \"properties\": {\"channel\": \"test\", \"webhookUrl\": \"https://example.com\"}}}");
        Response response = TestUtils.addProcessorToBridgeWithRequestBody(bridgeResponse.getId(), requestBody);
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridgeAndRetrieve() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), response.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName()).isEqualTo("myProcessor");
        assertThat(retrieved.getFilters().size()).isEqualTo(1);
        assertThat(retrieved.getTransformationTemplate()).isNull();
        assertRequestedAction(retrieved);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_bridgeDoesNotExist() {
        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_bridgeNotInReadyStatus() {
        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorToBridgeNoAuthentication() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", null, null, createKafkaAction())).as(ProcessorResponse.class);

        TestUtils.deleteProcessor(bridgeResponse.getId(), processorResponse.getId()).then().statusCode(202);
        processorResponse = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);

        assertThat(processorResponse.getStatus()).isEqualTo(ManagedResourceStatus.DEPROVISION);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteNotExistingProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();
        TestUtils.deleteProcessor(bridgeResponse.getId(), "not-existing").then().statusCode(404);
    }

    @Test
    public void testDeleteProcessorNoAuthentication() {
        TestUtils.deleteProcessor("any-id", "any-id").then().statusCode(401);
    }

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        BridgeResponse bridgeResponse = TestUtils.createBridge(r).as(BridgeResponse.class);
        return bridgeResponse;
    }

    private BridgeResponse createAndDeployBridge() {
        BridgeResponse bridgeResponse = createBridge();

        //Wait for the Bridge to be provisioned
        final List<BridgeDTO> bridges = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridges.clear();
            bridges.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridges.size()).isEqualTo(1);
        });

        //Emulate Shard updating Bridge status
        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridgeResponse.getId());
        dto.setStatus(ManagedResourceStatus.READY);
        dto.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        dto.setEndpoint("https://foo.bridges.redhat.com");

        Response deployment = TestUtils.updateBridge(dto);
        assertThat(deployment.getStatusCode()).isEqualTo(200);
        return bridgeResponse;
    }
}
