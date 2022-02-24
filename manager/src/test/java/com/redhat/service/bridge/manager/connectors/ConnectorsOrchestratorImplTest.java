package com.redhat.service.bridge.manager.connectors;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.connector.models.Connector;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.ShardService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.actions.connectors.SlackAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.mutiny.core.eventbus.Message;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConnectorsOrchestratorImplTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ProcessorService processorService;

    @InjectMock
    ConnectorsApiClient connectorsApiClient;

    @InjectMock
    RhoasService rhoasService;

    @Inject
    ConnectorsOrchestrator connectorsOrchestrator;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    ShardService shardService;

    @BeforeEach
    public void before() {
        databaseManagerUtils.cleanUp();
    }

    private Bridge createPersistBridge(BridgeStatus status, String bridgeName) {
        Bridge b = new Bridge();
        b.setName(bridgeName);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(status);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    private BaseAction createSlackAction() {
        BaseAction mcAction = new BaseAction();
        mcAction.setType(SlackAction.TYPE);
        Map<String, String> parameters = mcAction.getParameters();
        parameters.put("channel", "channel");
        parameters.put("webhookUrl", "webhook_url");
        return mcAction;
    }

    @Test
    public void testAvoidDoubleUpdate() {
        Bridge b = createPersistBridge(BridgeStatus.READY, "bridgeDoubleUpdate");

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessorDoubleUpdate", slackAction);

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        Connector connector = stubbedExternalConnector("connectorExternalId");
        when(connectorsApiClient.getConnector(any())).thenReturn(connector);
        when(connectorsApiClient.createConnector(any())).thenReturn(connector);
        when(shardService.getAssignedShardId(anyString())).thenReturn("myId");

        connectorsOrchestrator.updatePendingConnectors();
        connectorsOrchestrator.updatePendingConnectors();
        connectorsOrchestrator.updatePendingConnectors();

        // Multiple calls should execute only once but ConnectorEntity shouldn't be in error state
        assertConnectorIsInStatus(processor, ConnectorStatus.READY, ConnectorStatus.READY);

        verify(rhoasService, times(1)).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, times(1)).createConnector(any());
    }

    @Test
    public void testEventPropagation() {
        Bridge b = createPersistBridge(BridgeStatus.READY, "bridge");

        BaseAction slackAction = createSlackAction();
        ProcessorRequest processorRequest = new ProcessorRequest("ManagedConnectorProcessor", slackAction);

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), processorRequest);

        Connector connector = stubbedExternalConnector("connectorExternalId");
        //The first two attempts to lookup the MC Connector will fail; but the third is successful
        when(connectorsApiClient.getConnector(any())).thenReturn(null, null, connector);
        when(connectorsApiClient.createConnector(any())).thenReturn(connector);
        when(shardService.getAssignedShardId(anyString())).thenReturn("myId");

        UniSubscriber<List<Message<ConnectorEntity>>> subscriber = UniHelper.toSubscriber(asyncResult -> {
            /* NOP */});

        //Check status following first lookup
        connectorsOrchestrator.updatePendingConnectors().subscribe().withSubscriber(subscriber);
        assertConnectorIsInStatus(processor, ConnectorStatus.MANAGED_CONNECTOR_LOOKUP_FAILED, ConnectorStatus.READY, "Connector not found on MC Fleet Manager");

        //Check status following second lookup
        connectorsOrchestrator.updatePendingConnectors().subscribe().withSubscriber(subscriber);
        assertConnectorIsInStatus(processor, ConnectorStatus.MANAGED_CONNECTOR_LOOKUP_FAILED, ConnectorStatus.READY, "Connector not found on MC Fleet Manager");

        //Check status following third and final lookup
        connectorsOrchestrator.updatePendingConnectors().subscribe().withSubscriber(subscriber);
        assertConnectorIsInStatus(processor, ConnectorStatus.READY, ConnectorStatus.READY);

        verify(rhoasService, times(1)).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, times(1)).createConnector(any());
    }

    private void assertConnectorIsInStatus(Processor processor, ConnectorStatus status, ConnectorStatus desiredStatus) {
        await().atMost(500, SECONDS).untilAsserted(() -> {
            ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(processor.getId(), String.format("OpenBridge-slack_sink_0.1-%s", processor.getId()));

            assertThat(foundConnector).isNotNull();
            assertThat(foundConnector.getError()).isNullOrEmpty();
            assertThat(foundConnector.getDesiredStatus()).isEqualTo(desiredStatus);
            assertThat(foundConnector.getStatus()).isEqualTo(status);
        });
    }

    private void assertConnectorIsInStatus(Processor processor, ConnectorStatus status, ConnectorStatus desiredStatus, String errorMessage) {
        await().atMost(500, SECONDS).untilAsserted(() -> {
            ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(processor.getId(),
                    String.format("OpenBridge-slack_sink_0.1-%s", processor.getId()));

            assertThat(foundConnector).isNotNull();
            assertThat(foundConnector.getError()).containsIgnoringCase(errorMessage);
            assertThat(foundConnector.getDesiredStatus()).isEqualTo(desiredStatus);
            assertThat(foundConnector.getStatus()).isEqualTo(status);
        });
    }

    private Connector stubbedExternalConnector(String connectorId) {
        Connector connector = new Connector();
        connector.setId(connectorId);
        return connector;
    }
}
