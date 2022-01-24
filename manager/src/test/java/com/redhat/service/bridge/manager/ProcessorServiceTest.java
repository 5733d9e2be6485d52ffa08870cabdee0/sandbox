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
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.manager.actions.connectors.SlackAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.connectors.ConnectorsServiceImpl;
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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMock
    ConnectorsApiClient connectorsApiClient;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    RhoasService rhoasServiceMock;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
        reset(rhoasServiceMock);
        when(rhoasServiceMock.isEnabled()).thenReturn(false);
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
        a.setName(TestConstants.DEFAULT_ACTION_NAME);
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);
        return a;
    }

    @Test
    public void createProcessor_bridgeNotActive() {
        Bridge b = createPersistBridge(BridgeStatus.PROVISIONING);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> processorService.createProcessor(b.getId(), b.getCustomerId(), new ProcessorRequest()));
    }

    @Test
    public void createProcessor_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.createProcessor(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID, new ProcessorRequest()));
    }

    @Test
    public void createProcessor_processorWithSameNameAlreadyExists() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(AlreadyExistingItemException.class).isThrownBy(() -> processorService.createProcessor(b.getId(), b.getCustomerId(), r));
    }

    @Test
    public void createProcessor() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", null, "{}", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThat(processor.getBridge().getId()).isEqualTo(b.getId());
        assertThat(processor.getName()).isEqualTo(r.getName());
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getDefinition()).isNotNull();

        ProcessorDefinition definition = jsonNodeToDefinition(processor.getDefinition());
        assertThat(definition.getTransformationTemplate()).isEqualTo("{}");
    }

    @Test
    @Transactional
    public void getProcessorByStatuses() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);

        r.setName("My Processor 2");
        processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        processor.setStatus(BridgeStatus.AVAILABLE);
        processorDAO.getEntityManager().merge(processor);

        r.setName("My Processor 3");
        processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        processor.setStatus(BridgeStatus.DELETION_REQUESTED);
        processorDAO.getEntityManager().merge(processor);

        List<Processor> processors = processorService.getProcessorByStatuses(asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED));
        assertThat(processors.size()).isEqualTo(2);
        processors.forEach((px) -> assertThat(px.getName()).isIn("My Processor", "My Processor 3"));
    }

    @Test
    public void updateProcessorStatus() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        ProcessorDTO dto = processorService.toDTO(processor);
        dto.setStatus(BridgeStatus.AVAILABLE);

        Processor updated = processorService.updateProcessorStatus(dto);
        assertThat(updated.getStatus()).isEqualTo(BridgeStatus.AVAILABLE);
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
        p.setBridge(createPersistBridge(BridgeStatus.AVAILABLE));
        p.setId("foo");

        ProcessorDTO processor = processorService.toDTO(p);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    public void getProcessor() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        Processor found = processorService.getProcessor(processor.getId(), b.getId(), b.getCustomerId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(processor.getId());
        assertThat(found.getBridge().getId()).isEqualTo(b.getId());
        assertThat(found.getBridge().getCustomerId()).isEqualTo(b.getCustomerId());
    }

    @Test
    public void getProcessor_bridgeDoesNotExist() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessor(processor.getId(), "doesNotExist", b.getCustomerId()));
    }

    @Test
    public void getProcessor_processorDoesNotExist() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessor("doesNotExist", b.getId(), b.getCustomerId()));
    }

    @Test
    public void getProcessors() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        ListResult<Processor> results = processorService.getProcessors(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);

        assertThat(results.getItems().get(0).getId()).isEqualTo(processor.getId());
    }

    @Test
    public void getProcessors_noProcessorsOnBridge() {

        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ListResult<Processor> results = processorService.getProcessors(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isZero();
        assertThat(results.getTotal()).isZero();
    }

    @Test
    public void getProcessors_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessors("doesNotExist", TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 100)));
    }

    @Test
    public void testGetProcessorsCount() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        Long result = processorService.getProcessorsCount(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testDeleteProcessor() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        processorService.deleteProcessor(b.getId(), processor.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        processor = processorService.getProcessor(processor.getId(), b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.DELETION_REQUESTED);
    }

    @Test
    public void testMGDOBR_80() {
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("name", "myName"));
        filters.add(new StringEquals("surename", "mySurename"));
        ProcessorRequest r = new ProcessorRequest("My Processor", filters, null, createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThat(processorService.getProcessors(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 100)).getSize()).isEqualTo(1);
    }

    @Test
    public void toResponse() {
        Bridge b = Fixtures.createBridge();
        Processor p = Fixtures.createProcessor(b, "foo");
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
        assertThat(r.getAction().getName()).isEqualTo(TestConstants.DEFAULT_ACTION_NAME);
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
        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        when(connectorsApiClient.createConnector(any())).thenReturn(stubbedExternalConnector("connectorExternalId"));
        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        ArgumentCaptor<ConnectorRequest> connectorCaptor = ArgumentCaptor.forClass(ConnectorRequest.class);
        verify(connectorsApiClient).createConnector(connectorCaptor.capture());
        ConnectorRequest calledConnector = connectorCaptor.getValue();
        assertThat(calledConnector.getKafka()).isNotNull();

        ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(processor.getId(), String.format("OpenBridge-slack_sink_0.1-%s", processor.getId()));
        assertThat(foundConnector).isNotNull();
    }

    @Test
    public void testDeleteProcessorWithConnector() {
        final String testConnectorId = "connectorExternalId";

        when(rhoasServiceMock.isEnabled()).thenReturn(true);
        when(connectorsApiClient.createConnector(any())).thenReturn(stubbedExternalConnector(testConnectorId));

        Bridge b = createPersistBridge(BridgeStatus.AVAILABLE);

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        assertThat(processor).isNotNull();
        verify(rhoasServiceMock).createTopicAndGrantAccessFor("ob-" + processor.getId(), RhoasTopicAccessType.PRODUCER);

        processorService.deleteProcessor(b.getId(), processor.getId(), TestConstants.DEFAULT_CUSTOMER_ID);

        verify(connectorsApiClient).deleteConnector(testConnectorId, ConnectorsServiceImpl.KAFKA_ID_IGNORED);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor("ob-" + processor.getId(), RhoasTopicAccessType.PRODUCER);

        ConnectorEntity connector = connectorsDAO.findByProcessorId(processor.getId());
        assertThat(connector).isNull();

        processor = processorService.getProcessor(processor.getId(), b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.DELETION_REQUESTED);
    }

    private Connector stubbedExternalConnector(String connectorExternalId) {
        Connector connector = new Connector();
        connector.setId(connectorExternalId);
        return connector;
    }

    private BaseAction createSlackAction() {
        BaseAction mcAction = new BaseAction();
        mcAction.setType(SlackAction.TYPE);
        Map<String, String> parameters = mcAction.getParameters();
        parameters.put("channel", "channel");
        parameters.put("webhookUrl", "webhook_url");
        return mcAction;
    }

}
