package com.redhat.service.smartevents.manager;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.connectors.ConnectorsApiClient;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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

/**
 * Test interaction between {@link ProcessorService} and Managed Connectors client APIs
 */
@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class ProcessorServiceConnectorTest {

    @Inject
    ProcessorService processorService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    BridgeDAO bridgeDAO;
    @Inject
    ProcessorDAO processorDAO;
    @Inject
    ConnectorsDAO connectorsDAO;

    @InjectMock
    ConnectorsApiClient connectorsApiClient;
    @InjectMock
    RhoasService rhoasService;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        reset(rhoasService);
    }

    @Test
    void createConnectorSuccess() {
        Bridge b = createPersistBridge(READY);

        String actionName = "actionName";
        Action slackAction = createSlackAction(actionName);
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        //Emulate successful External Connector creation
        Connector externalConnector = new Connector() {
            @NotNull
            @Override
            public Object getConnector() {
                // Ensure the mocked ManagedConnector definition matches reality to avoid a call to patch the definition.
                return connectorsDAO.findAll().firstResult().getDefinition();
            }
        };
        externalConnector.setId("connectorExternalId");
        ConnectorStatusStatus externalConnectorStatus = new ConnectorStatusStatus();
        externalConnectorStatus.setState(ConnectorState.READY);
        externalConnector.setStatus(externalConnectorStatus);

        when(connectorsApiClient.getConnector(any())).thenReturn(externalConnector);
        when(connectorsApiClient.createConnector(any(ConnectorEntity.class))).thenCallRealMethod();
        when(connectorsApiClient.createConnector(any(ConnectorRequest.class))).thenReturn(externalConnector);
        when(rhoasService.createTopicAndGrantAccessFor(anyString(), any())).thenReturn(new Topic());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), b.getOwner(), processorRequest);

        //There will be 2 re-tries at 5s each. Add 5s to be certain everything completes.
        await().atMost(15, SECONDS).untilAsserted(() -> {
            ConnectorEntity connector = connectorsDAO.findByProcessorIdAndName(processor.getId(),
                    resourceNamesProvider.getProcessorConnectorName(processor.getId(), actionName));

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
    void createConnectorFailureOnKafkaTopicCreation() {
        Bridge b = createPersistBridge(READY);

        Action slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        when(rhoasService.createTopicAndGrantAccessFor(anyString(), any())).thenThrow(
                new InternalPlatformException(RhoasServiceImpl.createFailureErrorMessageFor("errorTopic"), new RuntimeException("error")));
        when(connectorsApiClient.createConnector(any(ConnectorRequest.class))).thenReturn(new Connector());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), b.getOwner(), processorRequest);

        waitForProcessorAndConnectorToFail(processor);

        verify(rhoasService, atLeast(1)).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, never()).createConnector(any(ConnectorRequest.class));
    }

    @Test
    void createConnectorFailureOnExternalConnectorCreation() {
        Bridge b = createPersistBridge(READY);

        Action slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        doThrow(new InternalPlatformException(RhoasServiceImpl.createFailureErrorMessageFor("errorDeletingConnector"), new RuntimeException("error")))
                .when(connectorsApiClient).deleteConnector(anyString());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), b.getOwner(), processorRequest);

        waitForProcessorAndConnectorToFail(processor);
    }

    @Test
    void testDeleteRequestedConnectorSuccess() {
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

        verify(rhoasService).deleteTopicAndRevokeAccessFor(TestConstants.DEFAULT_KAFKA_TOPIC, RhoasTopicAccessType.PRODUCER);
        verify(connectorsApiClient).deleteConnector("connectorExternalId");

        assertShardAsksForProcessorToBeDeletedIncludes(processor);
    }

    @Test
    void testDeleteConnectorFailureOnKafkaTopicDeletion() {
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

        verify(rhoasService, atLeast(1)).deleteTopicAndRevokeAccessFor(TestConstants.DEFAULT_KAFKA_TOPIC, RhoasTopicAccessType.PRODUCER);
        verify(connectorsApiClient, atLeast(1)).deleteConnector(anyString());

        assertShardAsksForProcessorToBeDeletedDoesNotInclude(processor);
    }

    @Test
    void testDeleteConnectorFailureOnExternalConnectorDestruction() {
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

    private Bridge createPersistBridge(ManagedResourceStatus status) {
        Bridge b = Fixtures.createBridge();
        b.setStatus(status);
        bridgeDAO.persist(b);
        return b;
    }

    private Processor createPersistProcessor(Bridge bridge, ManagedResourceStatus status) {
        Processor processor = Fixtures.createProcessor(bridge, status);
        processorDAO.persist(processor);
        return processor;
    }

    private ConnectorEntity createPersistentConnector(Processor processor, ManagedResourceStatus status) {
        ConnectorEntity connector = Fixtures.createSinkConnector(processor, status);
        connectorsDAO.persist(connector);
        return connector;
    }

    private Action createSlackAction(String name) {
        Action mcAction = new Action();
        mcAction.setName(name);
        mcAction.setType(SlackAction.TYPE);
        mcAction.setMapParameters(Map.of("slack_channel", "channel",
                "slack_webhook_url", "webhook_url"));
        return mcAction;
    }

    private Action createSlackAction() {
        return createSlackAction("slackAction-name");
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
