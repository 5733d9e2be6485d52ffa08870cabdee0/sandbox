package com.redhat.service.smartevents.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.filters.StringBeginsWith;
import com.redhat.service.smartevents.infra.models.filters.StringContains;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.filters.ValuesIn;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.connectors.ConnectorsService;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;
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
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PROCESSOR_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test {@link ProcessorService} methods mocking all dependencies
 */
@QuarkusTest
class ProcessorServiceTest {

    public static final String NEW_PROCESSOR_NAME = "My Processor";
    public static final String NON_EXISTING_BRIDGE_ID = "non-existing-bridge-id";
    public static final String NOT_READY_BRIDGE_ID = "not-ready-bridge-id";
    public static final String NON_EXISTING_PROCESSOR_ID = "non-existing-processor-id";
    public static final String PROVISIONING_PROCESSOR_ID = "provisioning-processor-id";
    public static final String PROVISIONING_PROCESSOR_NAME = "provisioning-processor-name";
    public static final String FAILED_PROCESSOR_ID = "failed-processor-id";
    public static final String FAILED_PROCESSOR_NAME = "failed-processor-name";
    public static final QueryInfo QUERY_INFO = new QueryInfo(0, 100);

    @Inject
    ProcessorService processorService;

    @InjectMock
    ProcessorDAO processorDAO;
    @InjectMock
    BridgesService bridgesServiceMock;
    @InjectMock
    ConnectorsService connectorServiceMock;
    @InjectMock
    WorkManager workManagerMock;

    @BeforeEach
    public void cleanUp() {
        reset(bridgesServiceMock);
        reset(processorDAO);

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
        when(bridgesServiceMock.getReadyBridge(NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID))
                .thenThrow(new BridgeLifecycleException("Bridge not ready"));
        when(bridgesServiceMock.getReadyBridge(not(or(eq(DEFAULT_BRIDGE_ID), eq(NOT_READY_BRIDGE_ID))), eq(DEFAULT_CUSTOMER_ID)))
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

    private static Stream<Arguments> createProcessorParams() {
        Object[][] arguments = {
                { new ProcessorRequest(NEW_PROCESSOR_NAME, createKafkaTopicAction()), ProcessorType.SINK },
                { new ProcessorRequest(NEW_PROCESSOR_NAME, createSlackSource()), ProcessorType.SOURCE }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("createProcessorParams")
    void testCreateProcessor_bridgeNotActive(ProcessorRequest request) {
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.createProcessor(NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("createProcessorParams")
    void testCreateProcessor_bridgeDoesNotExist(ProcessorRequest request) {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.createProcessor(NON_EXISTING_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("createProcessorParams")
    void testCreateProcessor_processorWithSameNameAlreadyExists(ProcessorRequest request) {
        request.setName(DEFAULT_PROCESSOR_NAME);
        assertThatExceptionOfType(AlreadyExistingItemException.class)
                .isThrownBy(() -> processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("createProcessorParams")
    void testCreateProcessor_noFilters(ProcessorRequest request, ProcessorType type) {
        doTestCreateProcessor(request, type);
    }

    @ParameterizedTest
    @MethodSource("createProcessorParams")
    void testCreateProcessor_multipleFilters(ProcessorRequest request, ProcessorType type) { // tests https://issues.redhat.com/browse/MGDOBR-80
        request.setFilters(Set.of(
                new StringEquals("name", "myName"),
                new StringEquals("surname", "mySurname")));
        doTestCreateProcessor(request, type);
    }

    private void doTestCreateProcessor(ProcessorRequest request, ProcessorType type) {
        Processor processor = processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, request);

        assertThat(processor.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(processor.getType()).isEqualTo(type);
        assertThat(processor.getName()).isEqualTo(request.getName());
        assertThat(processor.getStatus()).isEqualTo(ACCEPTED);
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getDefinition()).isNotNull();

        ArgumentCaptor<Processor> processorCaptor1 = ArgumentCaptor.forClass(Processor.class);
        verify(processorDAO, times(1)).persist(processorCaptor1.capture());
        assertThat(processorCaptor1.getValue()).isEqualTo(processor);

        ArgumentCaptor<Processor> processorCaptor2 = ArgumentCaptor.forClass(Processor.class);
        verify(connectorServiceMock, times(1)).createConnectorEntity(processorCaptor2.capture());
        assertThat(processorCaptor2.getValue()).isEqualTo(processor);
        assertThat(processorCaptor2.getValue().getDefinition().getRequestedAction()).isEqualTo(request.getAction());

        ArgumentCaptor<Processor> processorCaptor3 = ArgumentCaptor.forClass(Processor.class);
        verify(workManagerMock, times(1)).schedule(processorCaptor3.capture());
        assertThat(processorCaptor3.getValue()).isEqualTo(processor);

        ProcessorDefinition definition = processor.getDefinition();
        assertThat(definition.getTransformationTemplate()).isEqualTo(request.getTransformationTemplate());
    }

    @Test
    void testGetProcessorByStatuses() {
        String processor1Name = NEW_PROCESSOR_NAME;

        Processor processor1 = new Processor();
        processor1.setType(ProcessorType.SINK);
        processor1.setName(processor1Name);
        processor1.setShardId(TestConstants.SHARD_ID);
        processor1.setStatus(ACCEPTED);
        processor1.setDependencyStatus(READY);

        String processor2Name = "My Processor 2";

        Processor processor2 = new Processor();
        processor2.setType(ProcessorType.SINK);
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
    void testUpdateProcessorStatus() {
        ProcessorDTO updateDto = new ProcessorDTO();
        updateDto.setType(DEFAULT_PROCESSOR_TYPE);
        updateDto.setId(DEFAULT_PROCESSOR_ID);
        updateDto.setBridgeId(DEFAULT_BRIDGE_ID);
        updateDto.setCustomerId(DEFAULT_CUSTOMER_ID);
        updateDto.setStatus(FAILED);

        Processor updated = processorService.updateProcessorStatus(updateDto);

        assertThat(updated.getStatus()).isEqualTo(FAILED);
    }

    @Test
    void testUpdateProcessorStatusReadyPublishedAt() {
        ProcessorDTO updateDto = new ProcessorDTO();
        updateDto.setType(DEFAULT_PROCESSOR_TYPE);
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
    void testUpdateProcessorStatus_bridgeDoesNotExist() {
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridgeId(NON_EXISTING_BRIDGE_ID);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    void testUpdateProcessorStatus_processorDoesNotExist() {
        ProcessorDTO updateDto = new ProcessorDTO();
        updateDto.setType(DEFAULT_PROCESSOR_TYPE);
        updateDto.setId(NON_EXISTING_PROCESSOR_ID);
        updateDto.setBridgeId(DEFAULT_BRIDGE_ID);
        updateDto.setCustomerId(DEFAULT_CUSTOMER_ID);
        updateDto.setStatus(READY);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(updateDto));
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
    void testGetProcessors() {
        ListResult<Processor> results = processorService.getProcessors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, QUERY_INFO);
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
    void testGetProcessorsCount() {
        Long result = processorService.getProcessorsCount(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID);
        assertThat(result).isEqualTo(3L);
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
        doTestDeleteProcessor(createReadyProcessor());
    }

    @Test
    void testDeleteProcessor_processorStatusIsFailed() {
        doTestDeleteProcessor(createFailedProcessor());
    }

    private void doTestDeleteProcessor(Processor processor) {
        processorService.deleteProcessor(DEFAULT_BRIDGE_ID, processor.getId(), DEFAULT_CUSTOMER_ID);

        ArgumentCaptor<Processor> processorCaptor1 = ArgumentCaptor.forClass(Processor.class);
        verify(connectorServiceMock).deleteConnectorEntity(processorCaptor1.capture());
        assertThat(processorCaptor1.getValue()).isEqualTo(processor);
        assertThat(processorCaptor1.getValue().getStatus()).isEqualTo(DEPROVISION);
        assertThat(processorCaptor1.getValue().getDeletedAt()).isNotNull();

        ArgumentCaptor<Processor> processorCaptor2 = ArgumentCaptor.forClass(Processor.class);
        verify(workManagerMock).schedule(processorCaptor2.capture());
        assertThat(processorCaptor2.getValue()).isEqualTo(processor);
        assertThat(processorCaptor2.getValue().getStatus()).isEqualTo(DEPROVISION);
        assertThat(processorCaptor1.getValue().getDeletedAt()).isNotNull();
    }

    private static Stream<Arguments> updateProcessorParams() {
        Object[] arguments = {
                new ProcessorRequest(DEFAULT_PROCESSOR_NAME, createKafkaTopicAction()),
                new ProcessorRequest(DEFAULT_PROCESSOR_NAME, createWebhookAction()),
                new ProcessorRequest(DEFAULT_PROCESSOR_NAME, createSlackSource())
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWhenBridgeNotExists(ProcessorRequest request) {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.updateProcessor(NON_EXISTING_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWhenBridgeNotInReadyState(ProcessorRequest request) {
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.updateProcessor(NOT_READY_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWhenProcessorNotExists(ProcessorRequest request) {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, NON_EXISTING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWhenProcessorNotInReadyState(ProcessorRequest request) {
        request.setName(PROVISIONING_PROCESSOR_NAME);
        assertThatExceptionOfType(ProcessorLifecycleException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, PROVISIONING_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWithName(ProcessorRequest request) {
        request.setName(request.getName() + "-updated");
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWithTemplate(ProcessorRequest request) {
        Processor existingProcessor = createReadyProcessorFromRequest(request);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(existingProcessor);

        String updatedTransformationTemplate = "template";
        request.setTransformationTemplate(updatedTransformationTemplate);

        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);
        ProcessorResponse updatedResponse = processorService.toResponse(updatedProcessor);
        assertThat(updatedResponse.getStatus()).isEqualTo(ACCEPTED);

        assertThat(updatedResponse.getFilters()).isNull();
        assertThat(updatedResponse.getTransformationTemplate()).isEqualTo(updatedTransformationTemplate);
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWithGateway(ProcessorRequest request) {
        Processor existingProcessor = createReadyProcessorFromRequest(request);

        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(existingProcessor);

        if (request.getType() == ProcessorType.SOURCE) {
            Source dummyNewSource = new Source();
            dummyNewSource.setType("DummySource");
            request.setSource(dummyNewSource);
        } else if (request.getType() == ProcessorType.SINK) {
            Action dummyNewAction = new Action();
            dummyNewAction.setType("DummyAction");
            request.setAction(dummyNewAction);
        }

        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWithGatewayWithOppositeType(ProcessorRequest request) {
        Processor existingProcessor = createReadyProcessorFromRequest(request);

        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(existingProcessor);

        if (request.getType() == ProcessorType.SOURCE) {
            Action dummyNewAction = new Action();
            dummyNewAction.setType("DummyAction");
            request.setAction(dummyNewAction);
            request.setSource(null);
        } else if (request.getType() == ProcessorType.SINK) {
            Source dummyNewSource = new Source();
            dummyNewSource.setType("DummySource");
            request.setSource(dummyNewSource);
            request.setAction(null);
        }

        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request));
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWithFilter(ProcessorRequest request) {
        Processor existingProcessor = createReadyProcessorFromRequest(request);
        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(existingProcessor);

        Set<BaseFilter> updatedFilters = Collections.singleton(new StringEquals("key", "value"));
        request.setFilters(updatedFilters);

        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);
        ProcessorResponse updatedResponse = processorService.toResponse(updatedProcessor);
        assertThat(updatedResponse.getStatus()).isEqualTo(ACCEPTED);

        assertThat(updatedResponse.getFilters()).hasSize(1);
        BaseFilter updatedFilter = updatedResponse.getFilters().iterator().next();
        assertThat(updatedFilter.getKey()).isEqualTo("key");
        assertThat(updatedFilter.getValue()).isEqualTo("value");
        assertThat(updatedResponse.getTransformationTemplate()).isNull();
    }

    @ParameterizedTest
    @MethodSource("updateProcessorParams")
    void testUpdateProcessorWithNoChange(ProcessorRequest request) {
        Set<BaseFilter> filters = Set.of(
                new StringBeginsWith("source", List.of("Storage")),
                new StringContains("source", List.of("StorageService")),
                new StringEquals("source", "StorageService"),
                new ValuesIn("source", List.of("StorageService")));
        request.setFilters(filters);

        Processor existingProcessor = createReadyProcessorFromRequest(request);

        when(processorDAO.findByIdBridgeIdAndCustomerId(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID))
                .thenReturn(existingProcessor);

        Processor updatedProcessor = processorService.updateProcessor(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, DEFAULT_CUSTOMER_ID, request);

        assertThat(updatedProcessor.getStatus()).isEqualTo(READY);
        assertThat(updatedProcessor).isEqualTo(existingProcessor);
    }

    @Test
    void testToResponse() {
        Bridge b = Fixtures.createBridge();
        Processor p = Fixtures.createProcessor(b, READY);
        Action action = Fixtures.createKafkaAction();

        ProcessorDefinition definition = new ProcessorDefinition(Collections.emptySet(), "", action);
        p.setDefinition(definition);

        ProcessorResponse r = processorService.toResponse(p);
        assertThat(r).isNotNull();

        assertThat(r.getHref()).isEqualTo(APIConstants.USER_API_BASE_PATH + b.getId() + "/processors/" + p.getId());
        assertThat(r.getName()).isEqualTo(p.getName());
        assertThat(r.getStatus()).isEqualTo(p.getStatus());
        assertThat(r.getType()).isEqualTo(p.getType());
        assertThat(r.getId()).isEqualTo(p.getId());
        assertThat(r.getSubmittedAt()).isEqualTo(p.getSubmittedAt());
        assertThat(r.getPublishedAt()).isEqualTo(p.getPublishedAt());
        assertThat(r.getKind()).isEqualTo("Processor");
        assertThat(r.getTransformationTemplate()).isEmpty();
        assertThat(r.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
    }

    private static Bridge createReadyBridge() {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(DEFAULT_BRIDGE_ID);
        bridge.setStatus(READY);
        return bridge;
    }

    private static Processor createReadyProcessor() {
        Processor processor = Fixtures.createProcessor(createReadyBridge(), READY);
        processor.setId(DEFAULT_PROCESSOR_ID);
        return processor;
    }

    private static Processor createReadyProcessorFromRequest(ProcessorRequest request) {
        ProcessorDefinition definition = request.getType() == ProcessorType.SOURCE
                ? new ProcessorDefinition(request.getFilters(), request.getTransformationTemplate(), request.getSource(), null)
                : new ProcessorDefinition(request.getFilters(), request.getTransformationTemplate(), request.getAction(), null);

        Processor processor = Fixtures.createProcessor(createReadyBridge(), READY);
        processor.setId(DEFAULT_PROCESSOR_ID);
        processor.setType(request.getType());
        processor.setName(request.getName());
        processor.setDefinition(definition);
        return processor;
    }

    private static Processor createProvisioningProcessor() {
        Processor processor = Fixtures.createProcessor(createReadyBridge(), PROVISIONING);
        processor.setId(PROVISIONING_PROCESSOR_ID);
        processor.setName(PROVISIONING_PROCESSOR_NAME);
        processor.setPublishedAt(null);
        return processor;
    }

    private static Processor createFailedProcessor() {
        Processor processor = Fixtures.createProcessor(createReadyBridge(), FAILED);
        processor.setId(FAILED_PROCESSOR_ID);
        processor.setName(FAILED_PROCESSOR_NAME);
        return processor;
    }

    private static Action createKafkaTopicAction() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        action.setParameters(Map.of(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC));
        return action;
    }

    private static Action createWebhookAction() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setParameters(Map.of(WebhookAction.ENDPOINT_PARAM, "https://webhook.site/a0704e8f-a817-4d02-b30a-b8c49d0132dc"));
        return action;
    }

    private static Source createSlackSource() {
        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setParameters(Map.of(
                SlackSource.CHANNEL_PARAM, "test-channel",
                SlackSource.TOKEN_PARAM, "test-token"));
        return source;
    }
}
