package com.redhat.service.smartevents.manager.v1.api.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v1.api.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.v1.api.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.v1.api.models.filters.StringIn;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v1.TestConstants;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v1.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v1.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v1.api.models.responses.ProcessorListResponse;
import com.redhat.service.smartevents.manager.v1.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v1.mocks.ProcessorRequestForTests;
import com.redhat.service.smartevents.manager.v1.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v1.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v1.utils.Fixtures;
import com.redhat.service.smartevents.manager.v1.utils.TestUtils;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType.SOURCE;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_USER_NAME;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ProcessorAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgeDAO bridgeDAO;

    @V1
    @Inject
    WorkManager workManager;

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
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    public void testAuthentication() {
        TestUtils.getProcessor(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 0, 100).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(2L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The first created is the last to be listed.
        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertThat(listResponse.getItems().get(1).getId()).isEqualTo(p1.getId());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createProcessorOrganisationWithNoQuota() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("organisation-with-no-quota");
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction()))
                .then().statusCode(402).body("kind", Matchers.equalTo("Errors"));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByName() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByName(bridgeResponse.getId(), "myProcessor2").as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByStatus() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), ACCEPTED);
        setProcessorStatus(p2.getId(), READY);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByStatus(bridgeResponse.getId(), READY).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByMultipleStatuses() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), ACCEPTED);
        setProcessorStatus(p2.getId(), READY);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByStatus(bridgeResponse.getId(), ACCEPTED, READY).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(2L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        listResponse.getItems().forEach((i) -> assertThat(i.getId()).isIn(p1.getId(), p2.getId()));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByStatusWithIncorrectValue() {
        // See JAX-RS 2.1 Specification, Section 3.2.
        // HTTP-404 is correct if the QueryString contains an invalid value.
        // If the field or property is annotated with @MatrixParam, @QueryParam or @PathParam then an implementation
        // MUST generate an instance of NotFoundException (404 status) that wraps the thrown exception...
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.listProcessorsFilterByStatusWithAnyValue(bridgeResponse.getId(), "banana").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createSlackSource())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByType(bridgeResponse.getId(), SOURCE).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByTypeWithIncorrectValue() {
        // See JAX-RS 2.1 Specification, Section 3.2.
        // HTTP-404 is correct if the QueryString contains an invalid value.
        // If the field or property is annotated with @MatrixParam, @QueryParam or @PathParam then an implementation
        // MUST generate an instance of NotFoundException (404 status) that wraps the thrown exception...
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.listProcessorsFilterByTypeWithAnyValue(bridgeResponse.getId(), "banana").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByNameAndStatus() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), ACCEPTED);
        setProcessorStatus(p2.getId(), READY);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByNameAndStatus(bridgeResponse.getId(), "myProcessor", READY).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByNameAndType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createSlackSource())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByNameAndType(bridgeResponse.getId(), "myProcessor", SOURCE).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByStatusAndType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createSlackSource())).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), READY);
        setProcessorStatus(p2.getId(), READY);

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByStatusAndType(bridgeResponse.getId(), READY, SOURCE).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors_pageOffset() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 1, 1).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isEqualTo(1L);
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The last page, 1, will contain the first processor.
        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p1.getId());
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

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse pr = response.as(ProcessorResponse.class);

        assertThat(pr.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(pr.getAction().getParameter(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TestConstants.DEFAULT_KAFKA_TOPIC);

        ProcessorResponse found = TestUtils.getProcessor(bridgeResponse.getId(), pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId()).isEqualTo(pr.getId());
        assertThat(found.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(found.getAction().getParameter(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessorWithSendToBridgeAction() {
        BridgeResponse bridgeResponse = createAndDeployBridge();
        String bridgeId = bridgeResponse.getId();

        Response response = TestUtils.addProcessorToBridge(bridgeId, new ProcessorRequest("myProcessor", TestUtils.createSendToBridgeAction(bridgeId)));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse pr = response.as(ProcessorResponse.class);

        assertThat(pr.getAction().getType()).isEqualTo(SendToBridgeAction.TYPE);
        assertThat(pr.getAction().getParameter(SendToBridgeAction.BRIDGE_ID_PARAM)).isEqualTo(bridgeId);

        ProcessorResponse found = TestUtils.getProcessor(bridgeId, pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId()).isEqualTo(pr.getId());
        assertThat(found.getAction().getType()).isEqualTo(SendToBridgeAction.TYPE);
        assertThat(found.getAction().getParameter(SendToBridgeAction.BRIDGE_ID_PARAM)).isEqualTo(bridgeId);
    }

    private void assertRequestedAction(ProcessorResponse processorResponse) {
        Action action = processorResponse.getAction();
        assertThat(action).isNotNull();
        assertThat(action.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(action.getParameter(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor_processorDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        Response found = TestUtils.getProcessor(bridgeResponse.getId(), "doesNotExist");
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor_bridgeDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

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
                new ProcessorRequest("myProcessor", filters, "{}", TestUtils.createKafkaAction()));
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

        Action action = TestUtils.createKafkaAction();
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

        Action action = TestUtils.createKafkaAction();
        action.setMapParameters(new HashMap<>());
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
                new ProcessorRequest("myProcessor", TestUtils.createKafkaAction()));
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
                new ProcessorRequest("myProcessor", Collections.singleton(new StringIn("pepe", null)), null, TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithEmptyChannelParameterToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Action action = TestUtils.createKafkaAction();
        action.setType(SlackAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "https://example.com");
        action.setMapParameters(params);

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", null, null, action));
        assertThat(response.getStatusCode()).isEqualTo(202);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithEmptyWebhookURLParameterToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Action action = TestUtils.createKafkaAction();
        action.setType(SlackAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "channel");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "");
        action.setMapParameters(params);

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", null, null, action));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithWrongFilterDefinitionToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        String requestBody = "{" +
                "\"name\": \"processorInvalid\"," +
                "\"action\": {" +
                "  \"type\": \"webhook_sink_0.1\"," +
                "  \"parameters\": {" +
                "    \"endpoint\": \"https://webhook.site/abcdef/\"" +
                "  }" +
                "}," +
                "\"filters\": [" +
                "  {" +
                "    \"type\": \"InvalidType\"," +
                "    \"key\": \"data.value\"," +
                "    \"value\": \"test\"" +
                "  }" +
                "]" +
                "}";

        Response response = TestUtils.addProcessorToBridgeWithRequestBody(bridgeResponse.getId(), requestBody);
        assertThat(response.getStatusCode()).isEqualTo(400);

        ErrorsResponse errors = response.as(ErrorsResponse.class);
        assertThat(errors.getItems()).hasSize(1);

        ErrorResponse error = errors.getItems().get(0);
        assertThat(error.getId()).isEqualTo("32");
        assertThat(error.getCode()).isEqualTo("OPENBRIDGE-32");
        assertThat(error.getReason()).contains("InvalidType");
        assertThat(error.getHref()).contains(V1APIConstants.V1_ERROR_API_BASE_PATH);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithWrongParametersNameToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        String requestBody =
                String.format("{\"name\": \"processorInvalid\", \"action\": {\"type\": \"Slack\", \"properties\": {\"slack_channel\": \"test\", \"slack_webhook_url\": \"https://example.com\"}}}");
        Response response = TestUtils.addProcessorToBridgeWithRequestBody(bridgeResponse.getId(), requestBody);
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithMalformedTemplateToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", null, "Malformed template {data.payload ", TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridgeAndRetrieve() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        Response response = TestUtils.addProcessorToBridge(
                bridgeResponse.getId(),
                new ProcessorRequest("myProcessor", filters, null, TestUtils.createKafkaAction()));
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
        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor", TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_bridgeNotInReadyStatus() {
        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        ProcessorRequestForTests request = new ProcessorRequestForTests();
        request.setAction(TestUtils.createKafkaAction());
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, request);
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
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(READY);
        bridgeDAO.persist(bridge);

        Processor processor = Fixtures.createProcessor(bridge, READY);
        processorDAO.persist(processor);

        TestUtils.deleteProcessor(bridge.getId(), processor.getId()).then().statusCode(202);
        ProcessorResponse processorResponse = TestUtils.getProcessor(bridge.getId(), processor.getId()).as(ProcessorResponse.class);

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

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenBridgeNotExists() {
        Response response = TestUtils.updateProcessor("non-existing",
                "anything",
                new ProcessorRequest("myProcessor", Collections.emptySet(), null, TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenBridgeNotInReadyState() {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(ManagedResourceStatus.PROVISIONING);
        bridgeDAO.persist(bridge);

        Response response = TestUtils.updateProcessor(bridge.getId(),
                "anything",
                new ProcessorRequest("myProcessor", Collections.emptySet(), null, TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenProcessorNotExists() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Response response = TestUtils.updateProcessor(bridge.getId(),
                "non-existing",
                new ProcessorRequest("myProcessor", Collections.emptySet(), null, TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenProcessorNotInReadyState() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.PROVISIONING);
        processorDAO.persist(processor);

        Response response = TestUtils.updateProcessor(bridge.getId(),
                processor.getId(),
                new ProcessorRequest(processor.getName(), Collections.emptySet(), null, TestUtils.createKafkaAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithName() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("key", "value"));
        Response createResponse = TestUtils.addProcessorToBridge(
                bridge.getId(),
                new ProcessorRequest("myProcessor", filters, null, TestUtils.createKafkaAction()));

        ProcessorResponse processor = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        setProcessorAsReady(processor.getId());

        Response response = TestUtils.updateProcessor(bridge.getId(),
                processor.getId(),
                new ProcessorRequest(processor.getName() + "-updated",
                        filters,
                        processor.getTransformationTemplate(),
                        processor.getAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithTemplate() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("key", "value"));
        Response createResponse = TestUtils.addProcessorToBridge(
                bridge.getId(),
                new ProcessorRequest("myProcessor", filters, "template", TestUtils.createKafkaAction()));

        // We have to wait until the existing Work to provision the Processor is complete.
        ProcessorResponse processorResponse = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        Processor processor = processorDAO.findById(processorResponse.getId());
        await().atMost(5, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            assertThat(workManager.exists(processor)).isFalse();
        });

        setProcessorAsReady(processorResponse.getId());
        Response response = TestUtils.updateProcessor(bridge.getId(),
                processorResponse.getId(),
                new ProcessorRequest(processorResponse.getName(),
                        filters,
                        "template-updated",
                        processorResponse.getAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse updated = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);

        assertThat(updated.getName()).isEqualTo("myProcessor");
        assertThat(updated.getFilters().size()).isEqualTo(1);
        BaseFilter updatedFilter = updated.getFilters().iterator().next();
        assertThat(updatedFilter.getKey()).isEqualTo("key");
        assertThat(updatedFilter.getValue()).isEqualTo("value");
        assertThat(updated.getTransformationTemplate()).isEqualTo("template-updated");
        assertRequestedAction(updated);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithMalformedTemplate() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Response createResponse = TestUtils.addProcessorToBridge(
                bridge.getId(),
                new ProcessorRequest("myProcessor", null, null, TestUtils.createKafkaAction()));

        ProcessorResponse processor = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        setProcessorAsReady(processor.getId());

        Response response = TestUtils.updateProcessor(bridge.getId(),
                processor.getId(),
                new ProcessorRequest(processor.getName(),
                        null,
                        "template {this.is.broken",
                        processor.getAction()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithAction() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("key", "value"));
        Response createResponse = TestUtils.addProcessorToBridge(
                bridge.getId(),
                new ProcessorRequest("myProcessor", filters, null, TestUtils.createKafkaAction()));

        ProcessorResponse processor = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        setProcessorAsReady(processor.getId());

        Response response = TestUtils.updateProcessor(bridge.getId(),
                processor.getId(),
                new ProcessorRequest(processor.getName(),
                        filters,
                        processor.getTransformationTemplate(),
                        null));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithFilter() {
        Bridge bridge = Fixtures.createBridge();
        bridgeDAO.persist(bridge);

        Set<BaseFilter> filters = Collections.singleton(new StringEquals("key", "value"));
        Response createResponse = TestUtils.addProcessorToBridge(
                bridge.getId(),
                new ProcessorRequest("myProcessor", filters, null, TestUtils.createKafkaAction()));

        // We have to wait until the existing Work to provision the Processor is complete.
        ProcessorResponse processorResponse = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        Processor processor = processorDAO.findById(processorResponse.getId());
        await().atMost(5, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            assertThat(workManager.exists(processor)).isFalse();
        });

        setProcessorAsReady(processorResponse.getId());
        Set<BaseFilter> updatedFilters = Set.of(new StringEquals("key1", "value1"), new StringEquals("key2", "value2"));
        Response response = TestUtils.updateProcessor(bridge.getId(),
                processorResponse.getId(),
                new ProcessorRequest(processorResponse.getName(),
                        updatedFilters,
                        processorResponse.getTransformationTemplate(),
                        processorResponse.getAction()));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse updated = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);

        assertThat(updated.getName()).isEqualTo("myProcessor");
        assertThat(updated.getFilters().size()).isEqualTo(2);
        assertThat(updated.getFilters().stream().filter(f -> f.getKey().equals("key1") && f.getValue().equals("value1")).count()).isEqualTo(1);
        assertThat(updated.getFilters().stream().filter(f -> f.getKey().equals("key2") && f.getValue().equals("value2")).count()).isEqualTo(1);
        assertThat(updated.getTransformationTemplate()).isNull();
        assertRequestedAction(updated);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void basicMetricsForBridgeAndProcessors() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor", TestUtils.createKafkaAction())).as(ProcessorResponse.class);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2", TestUtils.createKafkaAction())).as(ProcessorResponse.class);

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        assertThat(metrics).isNotNull();
        assertThat(metrics)
                .contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v1/bridges\",}")
                .contains("http_server_requests_seconds_sum{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v1/bridges\",}")
                .contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v1/bridges/" + bridgeResponse.getId() + "/processors\",}")
                .contains("http_server_requests_seconds_sum{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v1/bridges/" + bridgeResponse.getId() + "/processors\",}");
    }

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION);
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
        BridgeDTO bridgeDTO = new BridgeDTO();
        bridgeDTO.setId(bridgeResponse.getId());
        bridgeDTO.setStatus(READY);
        bridgeDTO.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        bridgeDTO.setEndpoint("https://foo.bridges.redhat.com");

        Response deployment = TestUtils.updateBridge(bridgeDTO);
        assertThat(deployment.getStatusCode()).isEqualTo(200);
        return bridgeResponse;
    }

    private void setProcessorAsReady(String processorId) {
        setProcessorStatus(processorId, READY);
    }

    @Transactional
    protected void setProcessorStatus(String processorId, ManagedResourceStatus status) {
        Processor processor = processorDAO.findById(processorId);
        processor.setStatus(status);
    }
}
