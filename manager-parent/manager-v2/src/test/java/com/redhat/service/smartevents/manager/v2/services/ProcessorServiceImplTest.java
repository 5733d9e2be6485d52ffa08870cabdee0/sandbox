package com.redhat.service.smartevents.manager.v2.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createDeprovisionProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createFailedBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createFailedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createFailedProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorDeletingConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorDeprovisionConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorPreparingConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorProvisioningConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProvisioningProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getManagedResourceStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorServiceImplTest {

    public static final String NEW_PROCESSOR_NAME = "My Processor";
    public static final String NOT_READY_BRIDGE_ID = "not-ready-bridge-id";
    public static final String PROVISIONING_PROCESSOR_ID = "provisioning-processor-id";
    public static final String PROVISIONING_PROCESSOR_NAME = "provisioning-processor-name";
    public static final String FAILED_PROCESSOR_ID = "failed-processor-id";
    public static final String FAILED_PROCESSOR_NAME = "failed-processor-name";

    public static final String NON_EXISTING_BRIDGE_ID = "non-existing-bridge-id";
    public static final String NON_EXISTING_PROCESSOR_ID = "non-existing-processor-id";
    public static final String NON_EXISTING_CUSTOMER_ID = "non-existing-customer-id";

    public static final QueryResourceInfo QUERY_INFO = new QueryResourceInfo(0, 100);

    @Inject
    ProcessorService processorService;

    @InjectMock
    ProcessorDAO processorDAO;

    @InjectMock
    BridgeService bridgeServiceMock;

    @V2
    @InjectMock
    WorkManager workManager;

    @BeforeEach
    public void cleanUp() {
        reset(bridgeServiceMock);
        reset(processorDAO);

        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        assertThat(getManagedResourceStatus(bridge)).isEqualTo(READY);

        Processor processor = createReadyProcessor(bridge);
        assertThat(getManagedResourceStatus(processor)).isEqualTo(READY);

        Processor provisioningProcessor = createProvisioningProcessor(bridge);
        provisioningProcessor.setId(PROVISIONING_PROCESSOR_ID);
        provisioningProcessor.setName(PROVISIONING_PROCESSOR_NAME);
        assertThat(getManagedResourceStatus(provisioningProcessor)).isEqualTo(PROVISIONING);

        Processor failedProcessor = createFailedProcessor(bridge);
        failedProcessor.setId(FAILED_PROCESSOR_ID);
        failedProcessor.setName(FAILED_PROCESSOR_NAME);
        assertThat(getManagedResourceStatus(failedProcessor)).isEqualTo(FAILED);

        when(bridgeServiceMock.getBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);
        when(bridgeServiceMock.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);
        when(bridgeServiceMock.getReadyBridge(NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Bridge not ready"));
        when(bridgeServiceMock.getReadyBridge(not(or(eq(DEFAULT_BRIDGE_ID), eq(NOT_READY_BRIDGE_ID))), eq(DEFAULT_CUSTOMER_ID))).thenThrow(new ItemNotFoundException("Bridge not found"));

        when(bridgeServiceMock.getBridge(not(eq(DEFAULT_BRIDGE_ID)), eq(DEFAULT_CUSTOMER_ID))).thenThrow(new ItemNotFoundException("Bridge not found"));
        when(bridgeServiceMock.getBridge(any(), not(eq(DEFAULT_CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Bridge not found"));

        when(processorDAO.findById(DEFAULT_PROCESSOR_ID)).thenReturn(processor);
        when(processorDAO.findById(PROVISIONING_PROCESSOR_ID)).thenReturn(provisioningProcessor);
        when(processorDAO.findByBridgeIdAndName(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_NAME)).thenReturn(processor);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID)).thenReturn(processor);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, PROVISIONING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID)).thenReturn(provisioningProcessor);

        when(processorDAO.findByBridgeIdAndCustomerId(eq(DEFAULT_BRIDGE_ID), eq(DEFAULT_CUSTOMER_ID), any()))
                .thenReturn(new ListResult<>(List.of(processor, provisioningProcessor, failedProcessor), 0, 3));
    }

    @Test
    void testCreateProcessor_bridgeNotActive() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName(NEW_PROCESSOR_NAME);
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.createProcessor(NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateProcessor_bridgeDoesNotExist() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName(NEW_PROCESSOR_NAME);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.createProcessor(NON_EXISTING_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateProcessor_processorWithSameNameAlreadyExists() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName(DEFAULT_PROCESSOR_NAME);
        assertThatExceptionOfType(AlreadyExistingItemException.class)
                .isThrownBy(() -> processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateProcessor_whiteSpaceInName() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName("   name   ");
        Processor processor = doTestCreateProcessor(request);
        assertThat(processor.getName()).isEqualTo("name");
    }

    @Test
    void testCreateProcessorOrganizationWithNoQuota() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName("name");
        assertThatExceptionOfType(NoQuotaAvailable.class)
                .isThrownBy(() -> processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, "organisation_with_no_quota", request));
    }

    private Processor doTestCreateProcessor(ProcessorRequest request) {
        Processor processor = processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request);
        doAssertProcessorCreation(processor, request);
        return processor;
    }

    private void doAssertProcessorCreation(Processor processor, ProcessorRequest request) {
        assertThat(processor.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(processor.getName()).isEqualTo(request.getName());
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getDefinition()).isNotNull();

        ArgumentCaptor<Processor> processorCaptor1 = ArgumentCaptor.forClass(Processor.class);
        verify(processorDAO, times(1)).persist(processorCaptor1.capture());
        assertThat(processorCaptor1.getValue()).isEqualTo(processor);

        ArgumentCaptor<Processor> processorCaptor2 = ArgumentCaptor.forClass(Processor.class);
        verify(workManager, times(1)).schedule(processorCaptor2.capture());
        assertThat(processorCaptor2.getValue()).isEqualTo(processor);

        assertThat(StatusUtilities.getManagedResourceStatus(processor)).isEqualTo(ACCEPTED);
        assertThat(processor.getOperation().getType()).isEqualTo(OperationType.CREATE);
        assertThat(processor.getOperation().getRequestedAt()).isNotNull();
    }

    @Test
    void testGetProcessor() {
        Processor found = processorService.getProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(DEFAULT_PROCESSOR_ID);
        assertThat(found.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(found.getBridge().getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
    }

    @Test
    void testGetProcessor_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessor(NON_EXISTING_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void testGetProcessor_processorDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessor(DEFAULT_BRIDGE_ID, NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void testGetProcessor_customerDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, NON_EXISTING_CUSTOMER_ID));
    }

    @Test
    void testGetProcessors() {
        ListResult<Processor> results = processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, QUERY_INFO);
        assertThat(results).isNotNull();
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(3L);
        assertThat(results.getTotal()).isEqualTo(3L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(DEFAULT_PROCESSOR_ID);
        assertThat(results.getItems().get(1).getId()).isEqualTo(PROVISIONING_PROCESSOR_ID);
        assertThat(results.getItems().get(2).getId()).isEqualTo(FAILED_PROCESSOR_ID);
    }

    @Test
    void testGetProcessors_noProcessorsOnBridge() {
        when(processorDAO.findByBridgeIdAndCustomerId(eq(DEFAULT_BRIDGE_ID), eq(DEFAULT_CUSTOMER_ID), any()))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        ListResult<Processor> results = processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, QUERY_INFO);
        assertThat(results).isNotNull();
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isZero();
        assertThat(results.getTotal()).isZero();
    }

    @Test
    void testGetProcessors_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessors(NON_EXISTING_BRIDGE_ID, DEFAULT_CUSTOMER_ID, QUERY_INFO));
    }

    @Test
    void testGetProcessorsWhenBridgeHasFailed() {
        Bridge bridge = createFailedBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);

        when(bridgeServiceMock.getBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);

        ListResult<Processor> results = processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, QUERY_INFO);
        assertThat(results).isNotNull();
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(3L);
        assertThat(results.getTotal()).isEqualTo(3L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(DEFAULT_PROCESSOR_ID);
        assertThat(results.getItems().get(1).getId()).isEqualTo(PROVISIONING_PROCESSOR_ID);
        assertThat(results.getItems().get(2).getId()).isEqualTo(FAILED_PROCESSOR_ID);
    }

    @ParameterizedTest
    @MethodSource("getProcessorsWhenBridgeHasConditions")
    void testGetProcessorsWhenBridgeHasConditions(List<Condition> conditions) {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridge.setConditions(conditions);

        when(bridgeServiceMock.getBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);

        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, QUERY_INFO));
    }

    private static Stream<Arguments> getProcessorsWhenBridgeHasConditions() {
        Object[][] arguments = {
                { createProcessorPreparingConditions() },
                { createProcessorProvisioningConditions() },
                { createProcessorDeprovisionConditions() },
                { createProcessorDeletingConditions() }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @Test
    void testUpdateProcessorWhenBridgeNotExists() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.updateProcessor(NON_EXISTING_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void testUpdateProcessorWhenBridgeNotInReadyState() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME);
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.updateProcessor(NOT_READY_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void testUpdateProcessorWhenProcessorNotExists() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void testUpdateProcessorWhenProcessorNotInReadyState() {
        ProcessorRequest request = new ProcessorRequest(PROVISIONING_PROCESSOR_NAME);
        assertThatExceptionOfType(ProcessorLifecycleException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, PROVISIONING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void testUpdateProcessorWithName() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME);
        Processor existingProcessor = createReadyProcessorFromRequest(request);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(existingProcessor);

        request.setName(request.getName() + "-updated");
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void testUpdateProcessorWithFlows() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME);
        Processor existingProcessor = createReadyProcessorFromRequest(request);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID)).thenReturn(existingProcessor);

        ObjectNode newFlows = new ObjectNode(JsonNodeFactory.instance);
        newFlows.set("flow", JsonNodeFactory.instance.textNode("Flow"));
        request.setFlows(newFlows);

        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);
        ProcessorResponse updatedResponse = processorService.toResponse(updatedProcessor);
        assertThat(updatedResponse.getStatus()).isEqualTo(ACCEPTED);

        assertThat(updatedResponse.getFlows()).isNotNull();
        ObjectNode updatedFlows = updatedResponse.getFlows();
        assertThat(updatedFlows.asText()).isEqualTo(newFlows.asText());

        ArgumentCaptor<Processor> processorCaptor = ArgumentCaptor.forClass(Processor.class);
        verify(workManager, times(1)).schedule(processorCaptor.capture());
        assertThat(processorCaptor.getValue()).isEqualTo(existingProcessor);

        assertThat(StatusUtilities.getManagedResourceStatus(existingProcessor)).isEqualTo(ACCEPTED);
        assertThat(existingProcessor.getOperation().getType()).isEqualTo(OperationType.UPDATE);
        assertThat(existingProcessor.getOperation().getRequestedAt()).isNotNull();
    }

    @Test
    void testUpdateProcessorWithNoChange() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME);

        Processor existingProcessor = createReadyProcessorFromRequest(request);

        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID)).thenReturn(existingProcessor);

        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);

        assertThat(StatusUtilities.getManagedResourceStatus(updatedProcessor)).isEqualTo(READY);
        assertThat(updatedProcessor).isEqualTo(existingProcessor);
    }

    private static Processor createReadyProcessorFromRequest(ProcessorRequest request) {
        ProcessorDefinition definition = new ProcessorDefinition(request.getFlows());

        Processor processor = Fixtures.createReadyProcessor(createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME));
        processor.setId(DEFAULT_PROCESSOR_ID);
        processor.setName(request.getName());
        processor.setDefinition(definition);
        return processor;
    }

    @Test
    void testDeleteProcessor_processorDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.deleteProcessor(DEFAULT_BRIDGE_ID, NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void testDeleteProcess_processorIsNotReady() {
        assertThatExceptionOfType(ProcessorLifecycleException.class)
                .isThrownBy(() -> processorService.deleteProcessor(DEFAULT_BRIDGE_ID, PROVISIONING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void testDeleteProcessor_processorStatusIsReady() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        doTestDeleteProcessor(createReadyProcessor(bridge));
    }

    @Test
    void testDeleteProcessor_processorStatusIsFailed() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        doTestDeleteProcessor(createFailedProcessor(bridge));
    }

    private void doTestDeleteProcessor(Processor processor) {
        processorService.deleteProcessor(DEFAULT_BRIDGE_ID, processor.getId(), DEFAULT_CUSTOMER_ID);

        ArgumentCaptor<Processor> processorCaptor = ArgumentCaptor.forClass(Processor.class);
        verify(workManager, times(1)).schedule(processorCaptor.capture());

        Processor parameter = processorCaptor.getValue();
        assertThat(parameter).isNotNull();
        assertThat(parameter).isEqualTo(processor);
        assertThat(StatusUtilities.getManagedResourceStatus(parameter)).isEqualTo(DEPROVISION);
        assertThat(parameter.getOperation().getType()).isEqualTo(OperationType.DELETE);
        assertThat(parameter.getOperation().getRequestedAt()).isNotNull();
    }

    @Test
    void testFindByShardIdToDeployOrDelete() {
        processorService.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);

        verify(processorDAO).findByShardIdToDeployOrDelete(eq(TestConstants.SHARD_ID));
    }

    @Test
    void testUpdateProcessorStatus_Create() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Processor processor = createProvisioningProcessor(bridge);

        assertThat(processor.getPublishedAt()).isNull();

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(processor.getId());
        statusDTO.setGeneration(processor.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        when(processorDAO.findByIdWithConditions(processor.getId())).thenReturn(processor);

        processorService.updateProcessorStatus(statusDTO);

        assertThat(processor.getPublishedAt()).isNotNull();
    }

    @Test
    void testUpdateProcessorStatus_CreateWithSuccessiveUpdate() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Processor processor = createProvisioningProcessor(bridge);

        assertThat(processor.getPublishedAt()).isNull();

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(processor.getId());
        statusDTO.setGeneration(processor.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        when(processorDAO.findByIdWithConditions(processor.getId())).thenReturn(processor);

        Processor updated = processorService.updateProcessorStatus(statusDTO);

        assertThat(updated.getPublishedAt()).isNotNull();

        Processor updated2 = processorService.updateProcessorStatus(statusDTO);

        assertThat(updated2.getPublishedAt()).isEqualTo(updated.getPublishedAt());
    }

    @Test
    void testUpdateProcessorStatus_CreateWhenIncomplete() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Processor processor = createProvisioningProcessor(bridge);

        assertThat(processor.getPublishedAt()).isNull();

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(processor.getId());
        statusDTO.setGeneration(processor.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.FALSE, ZonedDateTime.now(ZoneOffset.UTC))));

        when(processorDAO.findByIdWithConditions(processor.getId())).thenReturn(processor);

        processorService.updateProcessorStatus(statusDTO);

        assertThat(processor.getPublishedAt()).isNull();
    }

    @Test
    @Disabled("Nothing to assert until Metrics are added back. See MGDOBR-1340.")
    void testUpdateProcessorStatus_Update() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Processor processor = createReadyProcessor(bridge);
        Operation operation = new Operation();
        operation.setType(OperationType.UPDATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        processor.setOperation(operation);

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(processor.getId());
        statusDTO.setGeneration(processor.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        when(processorDAO.findByIdWithConditions(processor.getId())).thenReturn(processor);

        processorService.updateProcessorStatus(statusDTO);

        assertThat(processor.getPublishedAt()).isNotNull();
    }

    @Test
    void testUpdateProcessorStatus_Delete() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Processor processor = createDeprovisionProcessor(bridge);

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(processor.getId());
        statusDTO.setGeneration(processor.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.FALSE, ZonedDateTime.now(ZoneOffset.UTC))));

        when(processorDAO.findByIdWithConditions(processor.getId())).thenReturn(processor);

        processorService.updateProcessorStatus(statusDTO);

        verify(processorDAO, never()).deleteById(eq(processor.getId()));
    }

    @Test
    void testUpdateProcessorStatus_DeleteWhenIncomplete() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Processor processor = createDeprovisionProcessor(bridge);

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(processor.getId());
        statusDTO.setGeneration(processor.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        when(processorDAO.findByIdWithConditions(processor.getId())).thenReturn(processor);

        processorService.updateProcessorStatus(statusDTO);

        verify(processorDAO).deleteById(eq(processor.getId()));
    }

    @Test
    public void testToDTO() {
        Bridge bridge = createBridge();
        Processor processor = createProcessor(bridge);
        processor.setOwner("Owner");
        processor.setConditions(createFailedConditions());

        ObjectNode flows = new ObjectNode(JsonNodeFactory.instance);
        flows.set("flow", JsonNodeFactory.instance.textNode("Flow"));
        processor.setDefinition(new ProcessorDefinition(flows));

        ProcessorDTO dto = processorService.toDTO(processor);

        assertThat(dto.getId()).isEqualTo(processor.getId());
        assertThat(dto.getBridgeId()).isEqualTo(processor.getBridge().getId());
        assertThat(dto.getCustomerId()).isEqualTo(processor.getBridge().getCustomerId());
        assertThat(dto.getOwner()).isEqualTo("Owner");
        assertThat(dto.getName()).isEqualTo(processor.getName());
        assertThat(dto.getFlows().asText()).isEqualTo(flows.asText());
        assertThat(dto.getOperationType()).isEqualTo(processor.getOperation().getType());
        assertThat(dto.getGeneration()).isEqualTo(processor.getGeneration());
    }

    @Test
    public void testToResponse() {
        Bridge bridge = createBridge();
        Processor processor = createProcessor(bridge);
        processor.setOwner("Owner");
        processor.setConditions(createFailedConditions());

        ObjectNode flows = new ObjectNode(JsonNodeFactory.instance);
        flows.set("flow", JsonNodeFactory.instance.textNode("Flow"));
        processor.setDefinition(new ProcessorDefinition(flows));

        ProcessorResponse response = processorService.toResponse(processor);

        assertThat(response.getId()).isEqualTo(processor.getId());
        assertThat(response.getName()).isEqualTo(processor.getName());
        assertThat(response.getStatus()).isEqualTo(FAILED);
        assertThat(response.getPublishedAt()).isEqualTo(processor.getPublishedAt());
        assertThat(response.getSubmittedAt()).isEqualTo(processor.getSubmittedAt());
        assertThat(response.getModifiedAt()).isNull();
        assertThat(response.getOwner()).isEqualTo("Owner");
        assertThat(response.getHref()).contains(V2APIConstants.V2_USER_API_BASE_PATH, bridge.getId());
        assertThat(response.getStatusMessage()).contains(TestConstants.FAILED_CONDITION_ERROR_CODE, TestConstants.FAILED_CONDITION_ERROR_MESSAGE);
        assertThat(response.getFlows().asText()).isEqualTo(flows.asText());
    }
}
