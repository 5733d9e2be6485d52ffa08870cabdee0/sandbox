package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.manager.actions.connectors.SlackAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.Fixtures;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;
import com.redhat.service.bridge.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.bridge.infra.models.dto.BridgeStatus.DEPROVISION;
import static com.redhat.service.bridge.infra.models.dto.BridgeStatus.FAILED;
import static com.redhat.service.bridge.manager.RhoasServiceImpl.createFailureErrorMessageFor;
import static com.redhat.service.bridge.manager.api.internal.ShardBridgesSyncAPI.SHARD_REQUESTED_STATUS;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ProcessorService processorService;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    RhoasService rhoasService;

    @InjectMock
    ConnectorsApiClient connectorsApiClient;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        reset(rhoasService);
    }

    private Bridge createPersistBridge(BridgeStatus status) {
        Bridge b = new Bridge();
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(status);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    private BaseAction createKafkaAction() {
        BaseAction a = new BaseAction();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);
        return a;
    }

    @Test
    public void createProcessor_bridgeNotActive() {
        Bridge b = createPersistBridge(BridgeStatus.PROVISIONING);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> processorService.createProcessor(b, new ProcessorRequest()));
    }

    @Test
    public void createProcessor_processorWithSameNameAlreadyExists() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(AlreadyExistingItemException.class).isThrownBy(() -> processorService.createProcessor(b, r));
    }

    @Test
    public void createProcessor() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", null, "{}", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        assertThat(processor.getBridge().getId()).isEqualTo(b.getId());
        assertThat(processor.getName()).isEqualTo(r.getName());
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.ACCEPTED);
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getDefinition()).isNotNull();

        ProcessorDefinition definition = jsonNodeToDefinition(processor.getDefinition());
        assertThat(definition.getTransformationTemplate()).isEqualTo("{}");
    }

    @Test
    @Transactional
    public void getProcessorByStatuses() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);

        r.setName("My Processor 2");
        processor = processorService.createProcessor(b, r);
        processor.setStatus(BridgeStatus.READY);
        processorDAO.getEntityManager().merge(processor);

        r.setName("My Processor 3");
        processor = processorService.createProcessor(b, r);
        processor.setStatus(DEPROVISION);
        processorDAO.getEntityManager().merge(processor);

        List<Processor> processors = processorService.getProcessorByStatusesAndShardIdWithReadyDependencies(asList(BridgeStatus.ACCEPTED, DEPROVISION), TestConstants.SHARD_ID);
        assertThat(processors.size()).isEqualTo(2);
        processors.forEach((px) -> assertThat(px.getName()).isIn("My Processor", "My Processor 3"));
    }

    @Test
    public void updateProcessorStatus() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        ProcessorDTO dto = processorService.toDTO(processor);
        dto.setStatus(BridgeStatus.READY);

        Processor updated = processorService.updateProcessorStatus(dto);
        assertThat(updated.getStatus()).isEqualTo(BridgeStatus.READY);
    }

    @Test
    public void updateProcessorStatus_bridgeDoesNotExist() {
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridgeId("foo");

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    public void updateProcessorStatus_processorDoesNotExist() {
        Processor p = new Processor();
        p.setBridge(createPersistBridge(BridgeStatus.READY));
        p.setId("foo");

        ProcessorDTO processor = processorService.toDTO(p);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    public void getProcessor() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        Processor found = processorService.getProcessor(processor.getId(), b);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(processor.getId());
        assertThat(found.getBridge().getId()).isEqualTo(b.getId());
        assertThat(found.getBridge().getCustomerId()).isEqualTo(b.getCustomerId());
    }

    @Test
    public void getProcessor_bridgeDoesNotExist() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        Bridge fakeBridge = new Bridge();
        fakeBridge.setId("doesNotExist");
        fakeBridge.setCustomerId(b.getCustomerId());
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessor(processor.getId(), fakeBridge));
    }

    @Test
    public void getProcessor_processorDoesNotExist() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessor("doesNotExist", b));
    }

    @Test
    public void getProcessors() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        ListResult<Processor> results = processorService.getProcessors(b, new QueryInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);

        assertThat(results.getItems().get(0).getId()).isEqualTo(processor.getId());
    }

    @Test
    public void getProcessors_noProcessorsOnBridge() {

        Bridge b = createPersistBridge(BridgeStatus.READY);
        ListResult<Processor> results = processorService.getProcessors(b, new QueryInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isZero();
        assertThat(results.getTotal()).isZero();
    }

    @Test
    public void getProcessors_bridgeDoesNotExist() {
        Bridge fakeBridge = new Bridge();
        fakeBridge.setId("doesNotExist");
        fakeBridge.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessors(fakeBridge, new QueryInfo(0, 100)));
    }

    @Test
    public void deleteProcessor_processorDoesNotExist() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.deleteProcessor(b, "doesNotExist"));
    }

    @Test
    public void testGetProcessorsCount() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        Long result = processorService.getProcessorsCount(b);
        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testDeleteProcessor() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        processorService.deleteProcessor(b, processor.getId());
        processor = processorService.getProcessor(processor.getId(), b);
        assertThat(processor.getStatus()).isEqualTo(DEPROVISION);

        assertShardAsksForProcessorToBeDeletedIncludes(processor);
    }

    @Test
    public void testMGDOBR_80() {
        Bridge b = createPersistBridge(BridgeStatus.READY);
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("name", "myName"));
        filters.add(new StringEquals("surename", "mySurename"));
        ProcessorRequest r = new ProcessorRequest("My Processor", filters, null, createKafkaAction());

        Processor processor = processorService.createProcessor(b, r);
        assertThat(processor).isNotNull();

        assertThat(processorService.getProcessors(b, new QueryInfo(0, 100)).getSize()).isEqualTo(1);
    }

    @Test
    public void toResponse() {
        Bridge b = Fixtures.createBridge();
        Processor p = Fixtures.createProcessor(b, "foo", BridgeStatus.READY);
        BaseAction action = Fixtures.createKafkaAction();

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
    void createConnector() {
        Bridge b = createPersistBridge(BridgeStatus.READY);

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        when(connectorsApiClient.createConnector(any())).thenReturn(new Connector());
        when(rhoasService.createTopicAndGrantAccessFor(anyString(), any())).thenReturn(new Topic());

        Processor processor = processorService.createProcessor(b, processorRequest);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            ConnectorEntity connector = connectorsDAO.findByProcessorIdAndName(processor.getId(),
                    String.format("OpenBridge-slack_sink_0.1-%s",
                            processor.getId()));

            assertThat(connector).isNotNull();
            assertThat(connector.getError()).isNullOrEmpty();
            assertThat(connector.getDesiredStatus()).isEqualTo(ConnectorStatus.READY);
            assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.READY);
        });

        verify(rhoasService).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));

        when(connectorsApiClient.createConnector(any())).thenReturn(stubbedExternalConnector("connectorExternalId"));
        ArgumentCaptor<ConnectorRequest> connectorCaptor = ArgumentCaptor.forClass(ConnectorRequest.class);
        verify(connectorsApiClient).createConnector(connectorCaptor.capture());
        ConnectorRequest calledConnector = connectorCaptor.getValue();
        assertThat(calledConnector.getKafka()).isNotNull();
    }

    @Test
    public void createConnectorFailureTopic() {
        Bridge b = createPersistBridge(BridgeStatus.READY);

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        when(rhoasService.createTopicAndGrantAccessFor(anyString(), any())).thenThrow(
                new InternalPlatformException(createFailureErrorMessageFor("errorTopic"), new RuntimeException("error")));
        when(connectorsApiClient.createConnector(any())).thenReturn(new Connector());

        Processor processor = processorService.createProcessor(b, processorRequest);
        List<ConnectorEntity> connectors = connectorsDAO.findByProcessorId(processor.getId());
        assertThat(connectors).hasSize(1);

        waitForProcessorAndConnectorToFail(connectors.get(0));

        verify(rhoasService).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, never()).createConnector(any());
    }

    @Test
    public void createConnectorFailureConnector() {
        Bridge b = createPersistBridge(BridgeStatus.READY);

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        doThrow(new InternalPlatformException(createFailureErrorMessageFor("errorDeletingConnector"), new RuntimeException("error")))
                .when(connectorsApiClient).deleteConnector(anyString());

        Processor processor = processorService.createProcessor(b, processorRequest);
        List<ConnectorEntity> connectors = connectorsDAO.findByProcessorId(processor.getId());
        assertThat(connectors).hasSize(1);

        waitForProcessorAndConnectorToFail(connectors.get(0));
    }

    @Test
    public void testDeleteRequestedConnector() {
        Bridge bridge = createPersistBridge(BridgeStatus.READY);
        Processor processor = Fixtures.createProcessor(bridge, "bridgeTestDelete", BridgeStatus.READY);
        ConnectorEntity connector = Fixtures.createConnector(processor,
                "connectorToBeDeleted",
                ConnectorStatus.READY,
                ConnectorStatus.READY,
                "topicName");
        processorDAO.persist(processor);
        connectorsDAO.persist(connector);

        processorService.deleteProcessor(bridge, processor.getId());

        reloadAssertProcessorIsInStatus(processor, DEPROVISION);

        waitForConnectorToBeDeleted(connector);

        verify(rhoasService).deleteTopicAndRevokeAccessFor(eq("topicName"), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient).deleteConnector("connectorExternalId");

        assertShardAsksForProcessorToBeDeletedIncludes(processor);
    }

    @Test
    public void testDeleteConnectorFailingTopic() {
        Bridge bridge = createPersistBridge(BridgeStatus.READY);
        Processor processor = Fixtures.createProcessor(bridge, "bridgeTestDelete", BridgeStatus.READY);
        ConnectorEntity connector = Fixtures.createConnector(processor,
                "connectorToBeDeleted",
                ConnectorStatus.READY,
                ConnectorStatus.READY,
                "topicName");
        processorDAO.persist(processor);
        connectorsDAO.persist(connector);

        doThrow(new InternalPlatformException(createFailureErrorMessageFor("errorTopic"), new RuntimeException("error")))
                .when(rhoasService).deleteTopicAndRevokeAccessFor(anyString(), any());

        processorService.deleteProcessor(bridge, processor.getId());

        reloadAssertProcessorIsInStatus(processor, DEPROVISION);

        waitForProcessorAndConnectorToFail(connector);

        verify(rhoasService).deleteTopicAndRevokeAccessFor(eq("topicName"), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, never()).deleteConnector(anyString());

        assertShardAsksForProcessorToBeDeletedDoesNotInclude(processor);
    }

    @Test
    public void testDeleteConnectorFailingConnector() {
        Bridge bridge = createPersistBridge(BridgeStatus.READY);
        Processor processor = Fixtures.createProcessor(bridge, "bridgeTestDelete", BridgeStatus.READY);
        ConnectorEntity connector = Fixtures.createConnector(processor,
                "connectorToBeDeleted",
                ConnectorStatus.READY,
                ConnectorStatus.READY,
                "topicName");
        processorDAO.persist(processor);
        connectorsDAO.persist(connector);

        doThrow(new InternalPlatformException(createFailureErrorMessageFor("errorDeletingConnector"), new RuntimeException("error")))
                .when(connectorsApiClient).deleteConnector(anyString());

        processorService.deleteProcessor(bridge, processor.getId());

        reloadAssertProcessorIsInStatus(processor, DEPROVISION);

        waitForProcessorAndConnectorToFail(connector);

        verify(rhoasService).deleteTopicAndRevokeAccessFor(eq("topicName"), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient).deleteConnector(anyString());

        assertShardAsksForProcessorToBeDeletedDoesNotInclude(processor);
    }

    private BaseAction createSlackAction() {
        BaseAction mcAction = new BaseAction();
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
        List<Processor> processorsToBeDeleted = processorDAO.findByStatusesAndShardIdWithReadyDependencies(SHARD_REQUESTED_STATUS, TestConstants.SHARD_ID);
        assertThat(processorsToBeDeleted.stream().map(Processor::getId)).contains(processor.getId());
    }

    private void assertShardAsksForProcessorToBeDeletedDoesNotInclude(Processor processor) {
        List<Processor> processorsToBeDeleted = processorDAO.findByStatusesAndShardIdWithReadyDependencies(SHARD_REQUESTED_STATUS, TestConstants.SHARD_ID);
        assertThat(processorsToBeDeleted.stream().map(Processor::getId)).doesNotContain(processor.getId());
    }

    private void waitForProcessorAndConnectorToFail(ConnectorEntity connector) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(connector.getProcessor().getId(), connector.getName());
            assertThat(foundConnector.getStatus()).isEqualTo(ConnectorStatus.FAILED);

            reloadAssertProcessorIsInStatus(connector.getProcessor(), BridgeStatus.FAILED);
        });
    }

    private void waitForConnectorToBeDeleted(ConnectorEntity connector) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(connector.getProcessor().getId(), "connectorToBeDeleted");
            assertThat(foundConnector).isNull();
        });
    }

    private void reloadAssertProcessorIsInStatus(Processor processor, BridgeStatus status) {
        Processor foundProcessor = processorDAO.findById(processor.getId());
        assertThat(foundProcessor.getStatus()).isEqualTo(status);
    }
}
