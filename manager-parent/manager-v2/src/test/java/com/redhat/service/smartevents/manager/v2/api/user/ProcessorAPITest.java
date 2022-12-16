package com.redhat.service.smartevents.manager.v2.api.user;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgeReadyConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorAcceptedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorReadyConditions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ProcessorAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @InjectMock
    JsonWebToken jwt;

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
        TestUtils.getProcessor(TestConstants.DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName()).isEqualTo("myProcessor");

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName()).isEqualTo("myProcessor");
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithWrongDefinitionToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        String requestBody = "{" +
                "\"name\": \"processorInvalid\"," +
                "\"flange\": {" +
                "  }" +
                "}";

        Response response = TestUtils.addProcessorToBridgeWithRequestBody(bridgeResponse.getId(), requestBody);
        assertThat(response.getStatusCode()).isEqualTo(400);

        ErrorsResponse errors = response.as(ErrorsResponse.class);
        assertThat(errors.getItems()).hasSize(1);

        ErrorResponse error = errors.getItems().get(0);
        assertThat(error.getId()).isEqualTo("21");
        assertThat(error.getCode()).isEqualTo("OPENBRIDGE-21");
        assertThat(error.getReason()).contains("Processor flows cannot be null");
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_bridgeDoesNotExist() {
        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void addProcessorToBridge_bridgeNotInReadyStatus() {
        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorToBridgeNoAuthentication() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createProcessorOrganisationWithNoQuota() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("organisation-with-no-quota");
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"))
                .then().statusCode(402).body("kind", Matchers.equalTo("Errors"));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse pr = response.as(ProcessorResponse.class);

        ProcessorResponse found = TestUtils.getProcessor(bridgeResponse.getId(), pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId()).isEqualTo(pr.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor_processorDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(202);

        Response found = TestUtils.getProcessor(bridgeResponse.getId(), "doesNotExist");
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessor_bridgeDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);

        Response found = TestUtils.getProcessor("doesNotExist", response.getId());
        assertThat(found.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 0, 100).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(2L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The first created is the last to be listed.
        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertThat(listResponse.getItems().get(1).getId()).isEqualTo(p1.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessorsFilterByName() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

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

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), createProcessorAcceptedConditions());
        setProcessorStatus(p2.getId(), createProcessorReadyConditions());

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

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), createProcessorAcceptedConditions());
        setProcessorStatus(p2.getId(), createProcessorReadyConditions());

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
    public void listProcessorsFilterByNameAndStatus() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        setProcessorStatus(p1.getId(), createProcessorAcceptedConditions());
        setProcessorStatus(p2.getId(), createProcessorReadyConditions());

        ProcessorListResponse listResponse = TestUtils.listProcessorsFilterByNameAndStatus(bridgeResponse.getId(), "myProcessor", READY).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors_pageOffset() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 1, 1).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage()).isEqualTo(1L);
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The last page, 1, will contain the first processor.
        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p1.getId());
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
    public void updateProcessorWhenBridgeNotExists() {
        Response response = TestUtils.updateProcessor("non-existing", "anything", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenBridgeNotInReadyState() {
        Bridge bridge = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Response response = TestUtils.updateProcessor(bridge.getId(), "anything", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenProcessorNotExists() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Response response = TestUtils.updateProcessor(bridge.getId(), "non-existing", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWhenProcessorNotInReadyState() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Processor processor = Fixtures.createProvisioningProcessor(bridge);
        processorDAO.persist(processor);

        Response response = TestUtils.updateProcessor(bridge.getId(), processor.getId(), new ProcessorRequest(processor.getName()));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithName() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ObjectNode flows = new ObjectNode(JsonNodeFactory.instance);
        flows.set("flow", JsonNodeFactory.instance.textNode("Flow"));
        Response createResponse = TestUtils.addProcessorToBridge(bridge.getId(), new ProcessorRequest("myProcessor", flows));

        ProcessorResponse processor = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        setProcessorStatus(processor.getId(), createProcessorReadyConditions());

        Response response = TestUtils.updateProcessor(bridge.getId(), processor.getId(), new ProcessorRequest(processor.getName() + "-updated", flows));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithFlows() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ObjectNode flows = new ObjectNode(JsonNodeFactory.instance);
        flows.set("flow", JsonNodeFactory.instance.textNode("Flow"));
        Response createResponse = TestUtils.addProcessorToBridge(bridge.getId(), new ProcessorRequest("myProcessor", flows));

        ProcessorResponse processor = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        setProcessorStatus(processor.getId(), createProcessorReadyConditions());

        ObjectNode updatedFlows = new ObjectNode(JsonNodeFactory.instance);
        updatedFlows.set("flow", JsonNodeFactory.instance.textNode("FlowUpdated"));
        Response updateResponse = TestUtils.updateProcessor(bridge.getId(), processor.getId(), new ProcessorRequest(processor.getName(), flows));
        assertThat(updateResponse.getStatusCode()).isEqualTo(202);

        ProcessorResponse updated = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);

        assertThat(updated.getName()).isEqualTo("myProcessor");
        assertThat(updated.getFlows().asText()).isEqualTo(updatedFlows.asText());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorWithMalformedFlows() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ObjectNode flows = new ObjectNode(JsonNodeFactory.instance);
        flows.set("flow", JsonNodeFactory.instance.textNode("Flow"));
        Response createResponse = TestUtils.addProcessorToBridge(bridge.getId(), new ProcessorRequest("myProcessor", flows));

        ProcessorResponse processor = TestUtils.getProcessor(bridge.getId(), createResponse.as(ProcessorResponse.class).getId()).as(ProcessorResponse.class);
        setProcessorStatus(processor.getId(), createProcessorReadyConditions());

        Response response = TestUtils.updateProcessor(bridge.getId(), processor.getId(), new ProcessorRequest(processor.getName(), null));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteProcessor() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Processor processor = Fixtures.createReadyProcessor(bridge);
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

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION);
        return TestUtils.createBridge(r).as(BridgeResponse.class);
    }

    protected BridgeResponse createAndDeployBridge() {
        BridgeResponse bridgeResponse = createBridge();
        setBridgeStatus(bridgeResponse.getId(), createBridgeReadyConditions());
        return bridgeResponse;
    }

    @Transactional
    protected void setBridgeStatus(String bridgeId, List<Condition> conditions) {
        Bridge bridge = bridgeDAO.findById(bridgeId);
        bridge.setConditions(conditions);
    }

    @Transactional
    protected void setProcessorStatus(String processorId, List<Condition> conditions) {
        Processor processor = processorDAO.findById(processorId);
        processor.setConditions(conditions);
    }
}
