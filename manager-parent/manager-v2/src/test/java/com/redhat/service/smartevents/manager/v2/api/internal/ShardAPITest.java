package com.redhat.service.smartevents.manager.v2.api.internal;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.SinkConnectorDTO;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SinkConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus.FAILED;
import static com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus.TRUE;
import static com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus.UNKNOWN;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_CONFIG_MAP_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_SECRET_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.PREPARING;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.PROVISIONING;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TYPE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.SHARD_ID;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ShardAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    SinkConnectorDAO sinkConnectorDAO;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    @SuppressWarnings("unused")
    RhoasService rhoasServiceMock;

    @V2
    @InjectMock
    @SuppressWarnings("unused")
    // Prevent WorkManager from kicking in as we'll mock all activities
    WorkManager workManager;

    @ConfigProperty(name = "rhose.metrics-name.operation-total-count")
    String operationTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-success-total-count")
    String operationTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-failure-total-count")
    String operationTotalFailureCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-duration-seconds")
    String operationDurationMetricName;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(SHARD_ID);
        when(jwt.containsClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesToDeploy() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse.getId());

        final List<BridgeDTO> bridgesToDeployOrDelete = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete.size()).isEqualTo(1);
        });

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);
        assertThat(bridge.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridge.getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(bridge.getEndpoint()).isNotNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create two Processors for the Bridge
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-1")).as(ProcessorResponse.class);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-2"));
        //Emulate the Processor1 dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse.getId());

        final List<ProcessorDTO> processors = new ArrayList<>();
        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    processors.clear();
                    processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
                    }));
                    assertThat(processors.size()).isEqualTo(1);
                });

        ProcessorDTO processor = processors.get(0);
        assertThat(processor.getName()).isEqualTo(processorResponse.getName());
        assertThat(processor.getBridgeId()).isEqualTo(bridgeResponse.getId());
        assertThat(processor.getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(processor.getOwner()).isEqualTo(DEFAULT_USER_NAME);
        assertThat(processor.getOperationType()).isEqualTo(OperationType.CREATE);
        assertThat(processor.getGeneration()).isEqualTo(0);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getSinkConnectors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create two Sink Connectors for the Bridge
        ConnectorRequest request = new ConnectorRequest(DEFAULT_CONNECTOR_NAME + "-1");
        request.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID + "1");
        request.setConnector(JsonNodeFactory.instance.objectNode());
        SinkConnectorResponse sinkConnectorResponse = TestUtils.addConnectorToBridge(bridgeResponse.getId(), request, ConnectorType.SINK).as(SinkConnectorResponse.class);

        request.setName(DEFAULT_CONNECTOR_TYPE_ID + "2");
        TestUtils.addConnectorToBridge(bridgeResponse.getId(), request, ConnectorType.SINK).as(SinkConnectorResponse.class);

        //Emulate the Connector1 dependencies being completed in the Manager and hence becoming visible to the Shard
        mockSinkConnectorControlPlaneActivitiesComplete(sinkConnectorResponse.getId());

        final List<SinkConnectorDTO> sinkConnectors = new ArrayList<>();
        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    sinkConnectors.clear();
                    sinkConnectors.addAll(TestUtils.getSinkConnectorsToDeployOrDelete().as(new TypeRef<List<SinkConnectorDTO>>() {
                    }));
                    assertThat(sinkConnectors.size()).isEqualTo(1);
                });

        SinkConnectorDTO sinkConnector = sinkConnectors.get(0);
        assertThat(sinkConnector.getName()).isEqualTo(sinkConnectorResponse.getName());
        assertThat(sinkConnector.getBridgeId()).isEqualTo(bridgeResponse.getId());
        assertThat(sinkConnector.getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(sinkConnector.getOwner()).isEqualTo(DEFAULT_USER_NAME);
        assertThat(sinkConnector.getOperationType()).isEqualTo(OperationType.CREATE);
        assertThat(sinkConnector.getGeneration()).isEqualTo(0);
    }

    @Transactional
    protected void mockBridgeControlPlaneActivitiesComplete(String bridgeId) {
        Bridge bridge = bridgeDAO.findByIdWithConditions(bridgeId);
        bridge.getConditions().stream().filter(c -> c.getComponent() == ComponentType.MANAGER).forEach(c -> c.setStatus(ConditionStatus.TRUE));
    }

    @Transactional
    protected void mockBridgeControlPlaneActivitiesComplete(String processorId, long generation) {
        Bridge bridge = bridgeDAO.findByIdWithConditions(processorId);
        bridge.getConditions().stream().filter(c -> c.getComponent() == ComponentType.MANAGER).forEach(c -> c.setStatus(TRUE));
        bridge.setGeneration(generation);
    }

    @Transactional
    protected void mockBridgeCreation(String bridgeId) {
        Bridge bridge = bridgeDAO.findByIdWithConditions(bridgeId);
        bridge.getConditions().forEach(c -> c.setStatus(TRUE));
    }

    @Transactional
    protected void mockProcessorControlPlaneActivitiesComplete(String processorId) {
        mockProcessorControlPlaneActivitiesComplete(processorId, 0);
    }

    @Transactional
    protected void mockProcessorControlPlaneActivitiesComplete(String processorId, long generation) {
        Processor processor = processorDAO.findByIdWithConditions(processorId);
        processor.getConditions().stream().filter(c -> c.getComponent() == ComponentType.MANAGER).forEach(c -> c.setStatus(TRUE));
        processor.setGeneration(generation);
    }

    @Transactional
    protected void mockSinkConnectorControlPlaneActivitiesComplete(String sinkConnectorId) {
        mockSinkConnectorControlPlaneActivitiesComplete(sinkConnectorId, 0);
    }

    @Transactional
    protected void mockSinkConnectorControlPlaneActivitiesComplete(String sinkConnectorId, long generation) {
        Connector connector = sinkConnectorDAO.findByIdWithConditions(sinkConnectorId);
        connector.getConditions().stream().filter(c -> c.getComponent() == ComponentType.MANAGER).forEach(c -> c.setStatus(TRUE));
        connector.setGeneration(generation);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeStatus() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse.getId());

        awaitForBridgeToBe(PREPARING, bridgeResponse.getId());

        // Update Status to reflect that the Bridge is being created by the Shard
        TestUtils.updateBridgeStatus(makeResourceStatusDTO(bridgeResponse.getId(),
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, UNKNOWN))));

        // Check the new status is PROVISIONING
        awaitForBridgeToBe(PROVISIONING, bridgeResponse.getId());

        // Update Status to reflect that the Bridge is READY
        TestUtils.updateBridgeStatus(makeResourceStatusDTO(bridgeResponse.getId(),
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))));

        // Check the new status remains READY
        awaitForBridgeToBe(READY, bridgeResponse.getId());
    }

    private void awaitForBridgeToBe(ManagedResourceStatusV2 status, String... bridgeIds) {
        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    Arrays.stream(bridgeIds).forEach(bridgeId -> {
                        Bridge b = bridgeDAO.findByIdWithConditions(bridgeId);
                        assertThat(StatusUtilities.getManagedResourceStatus(b)).isEqualTo(status);
                    });
                });
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeStatusWithBatch() {
        BridgeResponse bridgeResponse1 = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);
        BridgeResponse bridgeResponse2 = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME + "1", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Bridge dependencies being completed in the Manager and hence becoming visible to the Shard
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse1.getId());
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse2.getId());

        awaitForBridgeToBe(PREPARING, bridgeResponse1.getId(), bridgeResponse2.getId());

        // Update Status to reflect that the Bridge is READY
        TestUtils.updateBridgesStatus(List.of(
                makeResourceStatusDTO(bridgeResponse1.getId(),
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))),
                makeResourceStatusDTO(bridgeResponse2.getId(),
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE)))));

        // Check the new status remains READY
        awaitForBridgeToBe(READY, bridgeResponse1.getId(), bridgeResponse2.getId());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeStatusWithBatchWithFailure() {
        BridgeResponse bridgeResponse1 = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);
        BridgeResponse bridgeResponse2 = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME + "1", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Bridge dependencies being completed in the Manager and hence becoming visible to the Shard
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse1.getId());
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse2.getId());

        awaitForBridgeToBe(PREPARING, bridgeResponse1.getId(), bridgeResponse2.getId());

        // Update Status to reflect that the Bridge is READY
        TestUtils.updateBridgesStatus(List.of(
                makeResourceStatusDTO("UnknownBridgeId",
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))),
                makeResourceStatusDTO(bridgeResponse2.getId(),
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE)))));

        // The Bridge1 update had an unknown BridgeId so will fail; leaving the resource in PREPARING
        awaitForBridgeToBe(PREPARING, bridgeResponse1.getId());
        // The Bridge2 update should have succeeded moving the resource to READY
        awaitForBridgeToBe(READY, bridgeResponse2.getId());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeStatusWithStaleUpdate() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Bridge dependencies being completed in the Manager and hence becoming visible to the Shard
        mockBridgeControlPlaneActivitiesComplete(bridgeResponse.getId(), 1);

        awaitForBridgeToBe(PREPARING, bridgeResponse.getId());

        // Update Status to reflect FAILURE within the Shard however the update is on a stale generation
        TestUtils.updateBridgeStatus(makeResourceStatusDTO(bridgeResponse.getId(),
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, FAILED),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, FAILED))));

        // Check the new status remains PREPARING
        awaitForBridgeToBe(PREPARING, bridgeResponse.getId());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateProcessorStatus() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create a Processor for the Bridge
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME)).as(ProcessorResponse.class);
        //Emulate the Processor dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse.getId());
        String processorId = processorResponse.getId();

        awaitForProcessorToBe(PREPARING, processorId);

        // Update Status to reflect that the Processor is being created by the Shard
        TestUtils.updateProcessorStatus(makeResourceStatusDTO(processorId,
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, UNKNOWN))));

        // Check the new status is PROVISIONING
        awaitForProcessorToBe(PROVISIONING, processorId);

        // Update Status to reflect that the Processor is READY
        TestUtils.updateProcessorStatus(makeResourceStatusDTO(processorId,
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))));

        // Check the new status remains READY
        awaitForProcessorToBe(READY, processorId);
    }

    private void awaitForProcessorToBe(ManagedResourceStatusV2 status, String... processorIds) {
        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    Arrays.stream(processorIds).forEach(processorId -> {
                        Processor p = processorDAO.findByIdWithConditions(processorId);
                        assertThat(StatusUtilities.getManagedResourceStatus(p)).isEqualTo(status);
                    });
                });
    }

    private ResourceStatusDTO makeResourceStatusDTO(String resourceId, List<ConditionDTO> statusConditions) {
        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(resourceId);
        statusDTO.setGeneration(0);
        statusDTO.setConditions(statusConditions);

        return statusDTO;
    }

    private ConditionDTO makeConditionDTO(String type, ConditionStatus status) {
        return new ConditionDTO(type, status, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateProcessorStatusWithBatch() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create a Processor for the Bridge
        ProcessorResponse processorResponse1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-1")).as(ProcessorResponse.class);
        ProcessorResponse processorResponse2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-2")).as(ProcessorResponse.class);
        //Emulate the Processor dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse1.getId());
        mockProcessorControlPlaneActivitiesComplete(processorResponse2.getId());
        String processor1Id = processorResponse1.getId();
        String processor2Id = processorResponse2.getId();

        awaitForProcessorToBe(PREPARING, processor1Id, processor2Id);

        // Update Status to reflect that the Processor is READY
        TestUtils.updateProcessorsStatus(List.of(
                makeResourceStatusDTO(processor1Id,
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))),
                makeResourceStatusDTO(processor2Id,
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE)))));

        // Check the new status remains READY
        awaitForProcessorToBe(READY, processor1Id, processor2Id);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateProcessorStatusWithBatchWithFailure() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create a Processor for the Bridge
        ProcessorResponse processorResponse1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-1")).as(ProcessorResponse.class);
        ProcessorResponse processorResponse2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-2")).as(ProcessorResponse.class);
        //Emulate the Processor dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse1.getId());
        mockProcessorControlPlaneActivitiesComplete(processorResponse2.getId());
        String processor1Id = processorResponse1.getId();
        String processor2Id = processorResponse2.getId();

        awaitForProcessorToBe(PREPARING, processor1Id, processor2Id);

        // Update Status to reflect that the Processor is READY
        TestUtils.updateProcessorsStatus(List.of(
                makeResourceStatusDTO("UnknownProcessorId",
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))),
                makeResourceStatusDTO(processor2Id,
                        List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                                makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE)))));

        // The Processor1 update had an unknown ProcessorId so will fail; leaving the resource in PREPARING
        awaitForProcessorToBe(PREPARING, processor1Id);
        // The Processor2 update should have succeeded moving the resource to READY
        awaitForProcessorToBe(READY, processor2Id);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateProcessorStatusWithStaleUpdate() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create a Processor for the Bridge
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME)).as(ProcessorResponse.class);
        //Emulate the Processor dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse.getId(), 1);
        String processorId = processorResponse.getId();

        awaitForProcessorToBe(PREPARING, processorId);

        // Update Status to reflect FAILURE within the Shard however the update is on a stale generation
        TestUtils.updateProcessorStatus(makeResourceStatusDTO(processorId,
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, FAILED),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, FAILED))));

        // Check the new status remains PREPARING
        awaitForProcessorToBe(PREPARING, processorId);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void metricsAreProducedForSuccessfulDeployment() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create a Processor for the Bridge
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME)).as(ProcessorResponse.class);
        //Emulate the Processor dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse.getId());
        String processorId = processorResponse.getId();

        awaitForProcessorToBe(PREPARING, processorId);

        // Update Status to reflect that the Processor has been created by the Shard
        TestUtils.updateProcessorStatus(makeResourceStatusDTO(processorId,
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, TRUE))));

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        // Not all metrics are recorded by this test. Only the "successful" path in this test.
        assertThat(metrics).contains(operationTotalCountMetricName);
        assertThat(metrics).contains(operationTotalSuccessCountMetricName);
        assertThat(metrics).contains(operationDurationMetricName);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void metricsAreProducedForFailedDeployment() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create a Processor for the Bridge
        ProcessorResponse processorResponse = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME)).as(ProcessorResponse.class);
        //Emulate the Processor dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse.getId());
        String processorId = processorResponse.getId();

        awaitForProcessorToBe(PREPARING, processorId);

        // Update Status to reflect that the Processor failed to be created by the Shard
        TestUtils.updateProcessorStatus(makeResourceStatusDTO(processorId,
                List.of(makeConditionDTO(DP_SECRET_READY_NAME, TRUE),
                        makeConditionDTO(DP_CONFIG_MAP_READY_NAME, FAILED))));

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        // Not all metrics are recorded by this test. Only the "failure" path in this test.
        assertThat(metrics).contains(operationTotalCountMetricName);
        assertThat(metrics).contains(operationTotalFailureCountMetricName);
        assertThat(metrics).contains(operationDurationMetricName);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testUnauthorizedRole() {
        reset(jwt);
        when(jwt.getClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("hacker");
        when(jwt.containsClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        TestUtils.getBridgesToDeployOrDelete().then().statusCode(403);
        TestUtils.updateBridgesStatus(new ArrayList<>()).then().statusCode(403);
        TestUtils.getProcessorsToDeployOrDelete().then().statusCode(403);
        TestUtils.updateProcessorsStatus(new ArrayList<>()).then().statusCode(403);
        TestUtils.getSinkConnectorsToDeployOrDelete().then().statusCode(403);
        TestUtils.updateSinkConnectorsStatus(new ArrayList<>()).then().statusCode(403);
    }
}
