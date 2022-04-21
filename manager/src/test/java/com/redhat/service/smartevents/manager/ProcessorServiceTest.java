package com.redhat.service.smartevents.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.connectors.ConnectorsApiClient;
import com.redhat.service.smartevents.manager.connectors.ConnectorsService;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.utils.TestUtils;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DELETED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PROCESSOR_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PROCESSOR_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
@QuarkusTestResource(PostgresResource.class)
class ProcessorServiceTest {

    public static final String TEST_NON_EXISTING_BRIDGE_ID = "non-existing-bridge-id";
    public static final String TEST_NOT_READY_BRIDGE_ID = "not-ready-bridge-id";
    public static final String TEST_NON_EXISTING_PROCESSOR_ID = "non-existing-processor-id";
    public static final String PROVISIONING_PROCESSOR_ID = "provisioning-processor-id";
    public static final String PROVISIONING_PROCESSOR_NAME = "provisioning-processor-name";
    public static final String FAILED_PROCESSOR_ID = "failed-processor-id";
    public static final String FAILED_PROCESSOR_NAME = "failed-processor-name";
    public static final QueryInfo TEST_QUERY_INFO = new QueryInfo(0, 100);

    @Inject
    ProcessorService processorService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    // TRY TO MOCK
    @Inject
    BridgeDAO bridgeDAO;
    @InjectMock
    ProcessorDAO processorDAO;
    @Inject
    ConnectorsDAO connectorsDAO;

    @InjectMock
    BridgesService bridgesServiceMock;
    @InjectMock
    ConnectorsService connectorServiceMock;
    @InjectMock
    ShardService shardServiceMock;
    @InjectMock
    WorkManager workManagerMock;

    // TRY TO REMOVE
    @InjectMock
    ConnectorsApiClient connectorsApiClient;
    @InjectMock
    RhoasService rhoasService;

    @BeforeEach
    public void cleanUp() {
        //        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        reset(rhoasService);
        reset(bridgesServiceMock);

        Bridge bridge = createReadyBridge();
        Processor processor = createReadyProcessor();
        Processor provisioningProcessor = createProvisioningProcessor();
        Processor failedProcessor = createFailedProcessor();

        when(bridgesServiceMock.getBridge(DEFAULT_BRIDGE_ID))
                .thenReturn(bridge);
        when(bridgesServiceMock.getBridge(not(eq(DEFAULT_BRIDGE_ID))))
                .thenThrow(new ItemNotFoundException("Bridge not found"));

        when(bridgesServiceMock.getBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(bridge);
        when(bridgesServiceMock.getBridge(not(eq(DEFAULT_BRIDGE_ID)), eq(DEFAULT_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(bridgesServiceMock.getBridge(any(), not(eq(DEFAULT_CUSTOMER_ID))))
                .thenThrow(new ItemNotFoundException("Bridge not found"));

        when(bridgesServiceMock.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(bridge);
        when(bridgesServiceMock.getReadyBridge(TEST_NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID))
                .thenThrow(new BridgeLifecycleException("Bridge not ready"));
        when(bridgesServiceMock.getReadyBridge(not(or(eq(DEFAULT_BRIDGE_ID), eq(TEST_NOT_READY_BRIDGE_ID))), eq(DEFAULT_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));

        when(processorDAO.findById(DEFAULT_PROCESSOR_ID))
                .thenReturn(processor);
        when(processorDAO.findByBridgeIdAndName(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_NAME))
                .thenReturn(processor);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(processor);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, PROVISIONING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(provisioningProcessor);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, FAILED_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(failedProcessor);
        when(processorDAO.findByBridgeIdAndCustomerId(eq(DEFAULT_BRIDGE_ID), eq(DEFAULT_CUSTOMER_ID), any()))
                .thenReturn(new ListResult<>(List.of(processor, provisioningProcessor, failedProcessor), 0, 3));
        when(processorDAO.countByBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(3L);
    }

    private Bridge createReadyBridge() {
        Bridge b = Fixtures.createBridge();
        b.setId(DEFAULT_BRIDGE_ID);
        b.setStatus(READY);
        return b;
    }

    private Bridge createPersistBridge(ManagedResourceStatus status) {
        Bridge b = Fixtures.createBridge();
        b.setStatus(status);
        bridgeDAO.persist(b);
        return b;
    }

    private Processor createReadyProcessor() {
        Processor processor = Fixtures.createProcessor(createReadyBridge(), READY);
        processor.setId(DEFAULT_PROCESSOR_ID);
        return processor;
    }

    private Processor createProvisioningProcessor() {
        Processor processor = Fixtures.createProcessor(createReadyBridge(), PROVISIONING);
        processor.setId(PROVISIONING_PROCESSOR_ID);
        processor.setName(PROVISIONING_PROCESSOR_NAME);
        return processor;
    }

    private Processor createFailedProcessor() {
        Processor processor = Fixtures.createProcessor(createReadyBridge(), FAILED);
        processor.setId(FAILED_PROCESSOR_ID);
        processor.setName(FAILED_PROCESSOR_NAME);
        return processor;
    }

    private Processor createPersistProcessor(Bridge bridge, ManagedResourceStatus status) {
        Processor processor = Fixtures.createProcessor(bridge, status);
        processorDAO.persist(processor);
        return processor;
    }

    private ConnectorEntity createPersistentConnector(Processor processor, ManagedResourceStatus status) {
        ConnectorEntity connector = Fixtures.createConnector(processor, status);
        connectorsDAO.persist(connector);
        return connector;
    }

    private Action createKafkaAction() {
        Action a = new Action();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);
        return a;
    }

    @Test
    void createProcessor_bridgeNotActive() {
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.createProcessor(TEST_NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID, new ProcessorRequest()));
    }

    @Test
    void createProcessor_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.createProcessor(TEST_NON_EXISTING_BRIDGE_ID, DEFAULT_CUSTOMER_ID, new ProcessorRequest()));
    }

    @Test
    void createProcessor_processorWithSameNameAlreadyExists() {
        ProcessorRequest r = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, createKafkaAction());
        assertThatExceptionOfType(AlreadyExistingItemException.class)
                .isThrownBy(() -> processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, r));
    }

    @Test
    void createProcessor() {
        ProcessorRequest r = new ProcessorRequest("My Processor", null, "{}", createKafkaAction());

        Processor processor = processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, r);

        assertThat(processor.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(processor.getName()).isEqualTo(r.getName());
        assertThat(processor.getStatus()).isEqualTo(ACCEPTED);
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getDefinition()).isNotNull();

        ArgumentCaptor<Processor> processorCaptor1 = ArgumentCaptor.forClass(Processor.class);
        verify(processorDAO).persist(processorCaptor1.capture());
        assertThat(processorCaptor1.getValue()).isEqualTo(processor);

        ArgumentCaptor<Processor> processorCaptor2 = ArgumentCaptor.forClass(Processor.class);
        ArgumentCaptor<Action> actionCaptor2 = ArgumentCaptor.forClass(Action.class);
        verify(connectorServiceMock).createConnectorEntity(processorCaptor2.capture(), actionCaptor2.capture());
        assertThat(processorCaptor2.getValue()).isEqualTo(processor);
        assertThat(actionCaptor2.getValue()).isEqualTo(r.getAction());

        ArgumentCaptor<Processor> processorCaptor3 = ArgumentCaptor.forClass(Processor.class);
        verify(workManagerMock).schedule(processorCaptor3.capture());
        assertThat(processorCaptor3.getValue()).isEqualTo(processor);

        ProcessorDefinition definition = jsonNodeToDefinition(processor.getDefinition());
        assertThat(definition.getTransformationTemplate()).isEqualTo("{}");
    }

    @Test
    void getProcessorByStatuses() {
        String processor1Name = "My Processor";

        Processor processor1 = new Processor();
        processor1.setName(processor1Name);
        processor1.setShardId(TestConstants.SHARD_ID);
        processor1.setStatus(ACCEPTED);
        processor1.setDependencyStatus(READY);

        String processor2Name = "My Processor 2";

        Processor processor2 = new Processor();
        processor2.setName(processor2Name);
        processor2.setShardId(TestConstants.SHARD_ID);
        processor2.setStatus(DEPROVISION);
        processor2.setDependencyStatus(DELETED);

        when(processorDAO.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID))
                .thenReturn(List.of(processor1, processor2));

        List<Processor> processors = processorService.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID);

        assertThat(processors).hasSize(2);
        processors.forEach((px) -> assertThat(px.getName()).isIn(processor1Name, processor2Name));
    }

    @Test
    void updateProcessorStatus() {
        ProcessorDTO updateDto = new ProcessorDTO();
        updateDto.setId(DEFAULT_PROCESSOR_ID);
        updateDto.setBridgeId(DEFAULT_BRIDGE_ID);
        updateDto.setCustomerId(DEFAULT_CUSTOMER_ID);
        updateDto.setStatus(FAILED);

        Processor updated = processorService.updateProcessorStatus(updateDto);

        assertThat(updated.getStatus()).isEqualTo(FAILED);
    }

    @Test
    void updateProcessorStatusReadyPublishedAt() {
        ProcessorDTO updateDto = new ProcessorDTO();
        updateDto.setId(DEFAULT_PROCESSOR_ID);
        updateDto.setBridgeId(DEFAULT_BRIDGE_ID);
        updateDto.setCustomerId(DEFAULT_CUSTOMER_ID);
        updateDto.setStatus(READY);

        Processor publishedProcessor = processorService.updateProcessorStatus(updateDto);

        assertThat(publishedProcessor.getStatus()).isEqualTo(READY);
        assertThat(publishedProcessor.getPublishedAt()).isNotNull();

        //Check calls to set PublishedAt at idempotent
        Processor publishedProcessor2 = processorService.updateProcessorStatus(updateDto);

        assertThat(publishedProcessor2.getStatus()).isEqualTo(READY);
        assertThat(publishedProcessor2.getPublishedAt()).isEqualTo(publishedProcessor.getPublishedAt());
    }

    @Test
    void updateProcessorStatus_bridgeDoesNotExist() {
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridgeId(TEST_NON_EXISTING_BRIDGE_ID);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    void updateProcessorStatus_processorDoesNotExist() {
        ProcessorDTO updateDto = new ProcessorDTO();
        updateDto.setId(TEST_NON_EXISTING_PROCESSOR_ID);
        updateDto.setBridgeId(DEFAULT_BRIDGE_ID);
        updateDto.setCustomerId(DEFAULT_CUSTOMER_ID);
        updateDto.setStatus(READY);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(updateDto));
    }

    @Test
    void getProcessor() {
        Processor found = processorService.getProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(DEFAULT_PROCESSOR_ID);
        assertThat(found.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(found.getBridge().getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
    }

    @Test
    void getProcessor_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessor(TEST_NON_EXISTING_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void getProcessor_processorDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessor(DEFAULT_BRIDGE_ID, TEST_NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void getProcessors() {
        ListResult<Processor> results = processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, TEST_QUERY_INFO);
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(DEFAULT_PROCESSOR_ID);
    }

    @Test
    void getProcessors_noProcessorsOnBridge() {
        when(processorDAO.findByBridgeIdAndCustomerId(eq(DEFAULT_BRIDGE_ID), eq(DEFAULT_CUSTOMER_ID), any()))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        ListResult<Processor> results = processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, TEST_QUERY_INFO);
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isZero();
        assertThat(results.getTotal()).isZero();
    }

    @Test
    void getProcessors_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.getProcessors(TEST_NON_EXISTING_BRIDGE_ID, DEFAULT_CUSTOMER_ID, TEST_QUERY_INFO));
    }

    @Test
    void getProcessorsCount() {
        Long result = processorService.getProcessorsCount(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID);
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void deleteProcessor_processorDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.deleteProcessor(DEFAULT_BRIDGE_ID, TEST_NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void deleteProcessor_processorStatusIsReady() {
        deleteProcessor(createReadyProcessor());
    }

    @Test
    void deleteProcessor_processorStatusIsFailed() {
        deleteProcessor(createFailedProcessor());
    }

    private void deleteProcessor(Processor processor) {
        processorService.deleteProcessor(DEFAULT_BRIDGE_ID, processor.getId(), DEFAULT_CUSTOMER_ID);

        ArgumentCaptor<Processor> processorCaptor1 = ArgumentCaptor.forClass(Processor.class);
        verify(connectorServiceMock).deleteConnectorEntity(processorCaptor1.capture());
        assertThat(processorCaptor1.getValue()).isEqualTo(processor);
        assertThat(processorCaptor1.getValue().getStatus()).isEqualTo(DEPROVISION);

        ArgumentCaptor<Processor> processorCaptor2 = ArgumentCaptor.forClass(Processor.class);
        verify(workManagerMock).schedule(processorCaptor2.capture());
        assertThat(processorCaptor2.getValue()).isEqualTo(processor);
        assertThat(processorCaptor2.getValue().getStatus()).isEqualTo(DEPROVISION);
    }

    @Test
    void updateProcessorWhenBridgeNotExists() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, Collections.emptySet(), null, null);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.updateProcessor(TEST_NON_EXISTING_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void updateProcessorWhenBridgeNotInReadyState() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, Collections.emptySet(), null, null);
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.updateProcessor(TEST_NOT_READY_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void updateProcessorWhenProcessorNotExists() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, Collections.emptySet(), null, null);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, TEST_NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void updateProcessorWhenProcessorNotInReadyState() {
        ProcessorRequest request = new ProcessorRequest(PROVISIONING_PROCESSOR_NAME, Collections.emptySet(), null, null);
        assertThatExceptionOfType(ProcessorLifecycleException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, PROVISIONING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void updateProcessorWithName() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-updated", Collections.emptySet(), null, null);
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void updateProcessorWithTemplate() {
        String updatedTransformationTemplate = "template";
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, null, updatedTransformationTemplate, null);
        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);
        ProcessorResponse updatedResponse = processorService.toResponse(updatedProcessor);
        assertThat(updatedResponse.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);

        assertThat(updatedResponse.getFilters()).isNull();
        assertThat(updatedResponse.getTransformationTemplate()).isEqualTo(updatedTransformationTemplate);
    }

    @Test
    void updateProcessorWithAction() {
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, Collections.emptySet(), null, createKafkaAction());
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @Test
    void updateProcessorWithFilter() {
        Set<BaseFilter> updatedFilters = Collections.singleton(new StringEquals("key", "value"));
        ProcessorRequest request = new ProcessorRequest(DEFAULT_PROCESSOR_NAME, updatedFilters, null, null);
        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);
        ProcessorResponse updatedResponse = processorService.toResponse(updatedProcessor);
        assertThat(updatedResponse.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);

        assertThat(updatedResponse.getFilters()).hasSize(1);
        BaseFilter updatedFilter = updatedResponse.getFilters().iterator().next();
        assertThat(updatedFilter.getKey()).isEqualTo("key");
        assertThat(updatedFilter.getValue()).isEqualTo("value");
        assertThat(updatedResponse.getTransformationTemplate()).isNull();
    }

    @Test
    public void updateProcessorWithNoChange() {
        Bridge b = createPersistBridge(ManagedResourceStatus.READY);
        Processor processor = createPersistProcessor(b, ManagedResourceStatus.READY);
        String transformationTemplate = processorService.toResponse(processor).getTransformationTemplate();
        assertThat(transformationTemplate).isNull();

        ProcessorRequest request = new ProcessorRequest(processor.getName(), null, null, null);
        processorService.updateProcessor(b.getId(),
                processor.getId(),
                b.getCustomerId(),
                request);

        Processor updatedProcessor = processorService.getProcessor(b.getId(), processor.getId(), b.getCustomerId());
        assertThat(updatedProcessor.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        assertThat(processor).isEqualTo(updatedProcessor);
    }

    @Test
    public void testMGDOBR_80() {
        Bridge b = createPersistBridge(READY);
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("name", "myName"));
        filters.add(new StringEquals("surename", "mySurename"));
        ProcessorRequest r = new ProcessorRequest("My Processor", filters, null, createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        TestUtils.waitForProcessorDependenciesToBeReady(processorDAO, processor);

        assertThat(processorService.getProcessors(b.getId(), DEFAULT_CUSTOMER_ID, TEST_QUERY_INFO).getSize()).isEqualTo(1);
    }

    @Test
    public void toResponse() {
        Bridge b = Fixtures.createBridge();
        Processor p = Fixtures.createProcessor(b, READY);
        Action action = Fixtures.createKafkaAction();

        ProcessorDefinition definition = new ProcessorDefinition(Collections.emptySet(), "", action);
        p.setDefinition(definitionToJsonNode(definition));

        ProcessorResponse r = processorService.toResponse(p);
        assertThat(r).isNotNull();

        assertThat(r.getHref()).isEqualTo(APIConstants.USER_API_BASE_PATH + b.getId() + "/processors/" + p.getId());
        assertThat(r.getName()).isEqualTo(p.getName());
        assertThat(r.getStatus()).isEqualTo(p.getStatus());
        assertThat(r.getId()).isEqualTo(p.getId());
        assertThat(r.getSubmittedAt()).isEqualTo(p.getSubmittedAt());
        assertThat(r.getPublishedAt()).isEqualTo(p.getPublishedAt());
        assertThat(r.getKind()).isEqualTo("Processor");
        assertThat(r.getTransformationTemplate()).isEmpty();
        assertThat(r.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
    }

    private JsonNode definitionToJsonNode(ProcessorDefinition definition) {
        ProcessorServiceImpl processorServiceImpl = (ProcessorServiceImpl) processorService;
        return processorServiceImpl.definitionToJsonNode(definition);
    }

    private ProcessorDefinition jsonNodeToDefinition(JsonNode jsonNode) {
        ProcessorServiceImpl processorServiceImpl = (ProcessorServiceImpl) processorService;
        return processorServiceImpl.jsonNodeToDefinition(jsonNode);
    }

    @Test
    void createConnectorSuccess() {
        Bridge b = createPersistBridge(READY);

        Action slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        //Emulate successful External Connector creation
        Connector externalConnector = stubbedExternalConnector("connectorExternalId");
        ConnectorStatusStatus externalConnectorStatus = new ConnectorStatusStatus();
        externalConnectorStatus.setState(ConnectorState.READY);
        externalConnector.setStatus(externalConnectorStatus);

        when(connectorsApiClient.getConnector(any())).thenReturn(externalConnector);
        when(connectorsApiClient.createConnector(any(ConnectorEntity.class))).thenCallRealMethod();
        when(connectorsApiClient.createConnector(any(ConnectorRequest.class))).thenReturn(externalConnector);
        when(rhoasService.createTopicAndGrantAccessFor(anyString(), any())).thenReturn(new Topic());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        //There will be 2 re-tries at 5s each. Add 5s to be certain everything completes.
        await().atMost(15, SECONDS).untilAsserted(() -> {
            ConnectorEntity connector = connectorsDAO.findByProcessorIdAndName(processor.getId(),
                    resourceNamesProvider.getProcessorConnectorName(processor.getId()));

            assertThat(connector).isNotNull();
            assertThat(connector.getError()).isNullOrEmpty();
            assertThat(connector.getStatus()).isEqualTo(READY);
        });

        verify(rhoasService, atLeast(1)).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));

        ArgumentCaptor<ConnectorRequest> connectorCaptor = ArgumentCaptor.forClass(ConnectorRequest.class);
        verify(connectorsApiClient).createConnector(connectorCaptor.capture());
        ConnectorRequest calledConnector = connectorCaptor.getValue();
        assertThat(calledConnector.getKafka()).isNotNull();
    }

    @Test
    public void createConnectorFailureOnKafkaTopicCreation() {
        Bridge b = createPersistBridge(READY);

        Action slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        when(rhoasService.createTopicAndGrantAccessFor(anyString(), any())).thenThrow(
                new InternalPlatformException(RhoasServiceImpl.createFailureErrorMessageFor("errorTopic"), new RuntimeException("error")));
        when(connectorsApiClient.createConnector(any(ConnectorRequest.class))).thenReturn(new Connector());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        waitForProcessorAndConnectorToFail(processor);

        verify(rhoasService, atLeast(1)).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, never()).createConnector(any(ConnectorRequest.class));
    }

    @Test
    public void createConnectorFailureOnExternalConnectorCreation() {
        Bridge b = createPersistBridge(READY);

        Action slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        doThrow(new InternalPlatformException(RhoasServiceImpl.createFailureErrorMessageFor("errorDeletingConnector"), new RuntimeException("error")))
                .when(connectorsApiClient).deleteConnector(anyString());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        waitForProcessorAndConnectorToFail(processor);
    }

    @Test
    public void testDeleteRequestedConnectorSuccess() {
        Bridge bridge = createPersistBridge(READY);
        Processor processor = createPersistProcessor(bridge, READY);
        createPersistentConnector(processor, READY);

        //Emulate successful External Connector creation
        Connector externalConnector = new Connector();
        final ConnectorStatusStatus externalConnectorStatus = new ConnectorStatusStatus();
        externalConnectorStatus.setState(ConnectorState.READY);
        externalConnector.setStatus(externalConnectorStatus);
        when(connectorsApiClient.getConnector(any())).thenReturn(externalConnector);

        //Emulate successful External Connector deletion
        doAnswer(i -> {
            externalConnectorStatus.setState(ConnectorState.DELETED);
            return null;
        }).when(connectorsApiClient).deleteConnector(any());

        processorService.deleteProcessor(bridge.getId(), processor.getId(), DEFAULT_CUSTOMER_ID);

        reloadAssertProcessorIsInStatus(processor, DEPROVISION);

        waitForConnectorToBeDeleted(bridge, processor);

        verify(rhoasService).deleteTopicAndRevokeAccessFor(eq(TestConstants.DEFAULT_KAFKA_TOPIC), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient).deleteConnector("connectorExternalId");

        assertShardAsksForProcessorToBeDeletedIncludes(processor);
    }

    @Test
    public void testDeleteConnectorFailureOnKafkaTopicDeletion() {
        Bridge bridge = createPersistBridge(READY);
        Processor processor = createPersistProcessor(bridge, READY);
        createPersistentConnector(processor, READY);

        //Emulate successful External Connector creation
        Connector externalConnector = new Connector();
        final ConnectorStatusStatus externalConnectorStatus = new ConnectorStatusStatus();
        externalConnectorStatus.setState(ConnectorState.READY);
        externalConnector.setStatus(externalConnectorStatus);
        when(connectorsApiClient.getConnector(any())).thenReturn(externalConnector);

        //Emulate successful External Connector deletion
        doAnswer(i -> {
            externalConnectorStatus.setState(ConnectorState.DELETED);
            return null;
        }).when(connectorsApiClient).deleteConnector(any());

        doThrow(new InternalPlatformException(RhoasServiceImpl.createFailureErrorMessageFor("errorTopic"), new RuntimeException("error")))
                .when(rhoasService).deleteTopicAndRevokeAccessFor(anyString(), any());

        processorService.deleteProcessor(bridge.getId(), processor.getId(), DEFAULT_CUSTOMER_ID);

        reloadAssertProcessorIsInStatus(processor, DEPROVISION);

        waitForProcessorAndConnectorToFail(processor);

        verify(rhoasService, atLeast(1)).deleteTopicAndRevokeAccessFor(eq(TestConstants.DEFAULT_KAFKA_TOPIC), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, atLeast(1)).deleteConnector(anyString());

        assertShardAsksForProcessorToBeDeletedDoesNotInclude(processor);
    }

    @Test
    public void testDeleteConnectorFailureOnExternalConnectorDestruction() {
        Bridge bridge = createPersistBridge(READY);
        Processor processor = createPersistProcessor(bridge, READY);
        createPersistentConnector(processor, READY);

        //Emulate successful External Connector creation
        Connector externalConnector = new Connector();
        final ConnectorStatusStatus externalConnectorStatus = new ConnectorStatusStatus();
        externalConnectorStatus.setState(ConnectorState.READY);
        externalConnector.setStatus(externalConnectorStatus);
        when(connectorsApiClient.getConnector(any())).thenReturn(externalConnector);

        doThrow(new InternalPlatformException(RhoasServiceImpl.createFailureErrorMessageFor("errorDeletingConnector"), new RuntimeException("error")))
                .when(connectorsApiClient).deleteConnector(anyString());

        processorService.deleteProcessor(bridge.getId(), processor.getId(), DEFAULT_CUSTOMER_ID);

        reloadAssertProcessorIsInStatus(processor, DEPROVISION);

        waitForProcessorAndConnectorToFail(processor);

        verify(rhoasService, never()).deleteTopicAndRevokeAccessFor(any(), any());
        verify(connectorsApiClient, atLeast(1)).deleteConnector(anyString());

        assertShardAsksForProcessorToBeDeletedDoesNotInclude(processor);
    }

    @Test
    public void testDeleteProcess_whenProcessorIsNotReady() {
        Bridge bridge = createPersistBridge(READY);
        Processor processor = createPersistProcessor(bridge, ManagedResourceStatus.PROVISIONING);
        assertThatExceptionOfType(ProcessorLifecycleException.class).isThrownBy(() -> processorService.deleteProcessor(bridge.getId(), processor.getId(), bridge.getCustomerId()));
    }

    private Action createSlackAction() {
        Action mcAction = new Action();
        mcAction.setType(SlackAction.TYPE);
        Map<String, String> parameters = mcAction.getParameters();
        parameters.put("channel", "channel");
        parameters.put("webhookUrl", "webhook_url");
        return mcAction;
    }

    private Connector stubbedExternalConnector(String connectorExternalId) {
        Connector connector = new Connector();
        connector.setId(connectorExternalId);
        return connector;
    }

    private void assertShardAsksForProcessorToBeDeletedIncludes(Processor processor) {
        List<Processor> processorsToBeDeleted = processorDAO.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID);
        assertThat(processorsToBeDeleted.stream().map(Processor::getId)).contains(processor.getId());
    }

    private void assertShardAsksForProcessorToBeDeletedDoesNotInclude(Processor processor) {
        List<Processor> processorsToBeDeleted = processorDAO.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID);
        assertThat(processorsToBeDeleted.stream().map(Processor::getId)).doesNotContain(processor.getId());
    }

    private void waitForProcessorAndConnectorToFail(final Processor processor) {
        //There will be 4 re-tries at 5s each. Add 5s to be certain everything completes.
        await().atMost(25, SECONDS).untilAsserted(() -> {
            Processor p = processorDAO.findById(processor.getId());

            assertThat(p).isNotNull();
            assertThat(p.getStatus()).isEqualTo(FAILED);
        });
    }

    private void waitForConnectorToBeDeleted(final Bridge bridge, final Processor processor) {
        //There will be 2 re-tries at 5s each. Add 5s to be certain everything completes.
        await().atMost(15, SECONDS).untilAsserted(() -> {
            ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(processor.getId(), TestConstants.DEFAULT_CONNECTOR_NAME);
            assertThat(foundConnector).isNull();

            final Processor processorDeleted = processorService.getProcessor(bridge.getId(), processor.getId(), DEFAULT_CUSTOMER_ID);
            assertThat(processorDeleted.getStatus()).isEqualTo(DEPROVISION);
        });
    }

    private void reloadAssertProcessorIsInStatus(Processor processor, ManagedResourceStatus status) {
        Processor foundProcessor = processorDAO.findById(processor.getId());
        assertThat(foundProcessor.getStatus()).isEqualTo(status);
    }
}
