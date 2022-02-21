package com.redhat.service.bridge.manager.connectors;

import java.time.ZonedDateTime;
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

        when(connectorsApiClient.createConnector(any())).thenReturn(stubbedExternalConnector("connectorExternalId"));
        when(shardService.getAssignedShardId(anyString())).thenReturn("myId");

        connectorsOrchestrator.updatePendingConnectors();
        connectorsOrchestrator.updatePendingConnectors();
        connectorsOrchestrator.updatePendingConnectors();

        // Multiple calls should execute only once but ConnectorEntity shouldn't be in error state
        assertConnectorIsInStatus(processor, ConnectorStatus.READY, ConnectorStatus.READY);

        verify(rhoasService, times(1)).createTopicAndGrantAccessFor(anyString(), eq(RhoasTopicAccessType.PRODUCER));
        verify(connectorsApiClient, times(1)).createConnector(any());
    }

    private void assertConnectorIsInStatus(Processor processor, ConnectorStatus status, ConnectorStatus desiredStatus) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(processor.getId(), String.format("OpenBridge-slack_sink_0.1-%s", processor.getId()));

            assertThat(foundConnector).isNotNull();
            assertThat(foundConnector.getError()).isNullOrEmpty();
            assertThat(foundConnector.getDesiredStatus()).isEqualTo(desiredStatus);
            assertThat(foundConnector.getStatus()).isEqualTo(status);
        });
    }

    private Connector stubbedExternalConnector(String connectorExternalId) {
        Connector connector = new Connector();
        connector.setId(connectorExternalId);
        return connector;
    }
}
