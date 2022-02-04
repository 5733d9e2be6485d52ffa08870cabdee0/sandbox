package com.redhat.service.bridge.manager.connectors;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.connector.models.Connector;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.actions.connectors.SlackAction;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.bridge.manager.connectors.ConnectorsServiceImpl.KAFKA_ID_IGNORED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConnectorsServiceTest {

    private static final String TEST_CONNECTOR_ID = "test-connector-id";
    private static final String TEST_CONNECTOR_EXTERNAL_ID = "test-connector-ext-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_NAME = "TestProcessor";
    private static final String TEST_ACTION_NAME = "TestAction";
    private static final String TEST_ACTION_CHANNEL = "testchannel";
    private static final String TEST_ACTION_WEBHOOK = "https://test.example.com/webhook";
    private static final String TEST_ACTION_TOPIC = "ob-" + TEST_PROCESSOR_ID;

    @Inject
    SlackAction slackAction;

    @Inject
    WebhookAction webhookAction;

    @Inject
    ConnectorsService connectorsService;

    @InjectMock
    ConnectorsApiClient connectorsApiClientMock;

    @InjectMock
    ConnectorsDAO connectorsDAOMock;

    @InjectMock
    RhoasService rhoasServiceMock;

    @Test
    @Transactional
    void doNotCreateConnector() {
        Optional<ConnectorEntity> connector = connectorsService.createConnectorIfNeeded(testWebhookAction(), testProcessor(), webhookAction);
        assertThat(connector).isEmpty();

        verify(connectorsApiClientMock, never()).createConnector(any());
        verify(connectorsDAOMock, never()).persist(any(ConnectorEntity.class));
        verify(rhoasServiceMock, never()).createTopicAndGrantAccessFor(any(), any());
    }

    @Test
    @Transactional
    void doCreateConnector() {
        when(rhoasServiceMock.isEnabled()).thenReturn(true);
        when(connectorsApiClientMock.createConnector(any())).thenReturn(testConnector());

        Optional<ConnectorEntity> connector = connectorsService.createConnectorIfNeeded(testKafkaAction(), testProcessor(), slackAction);
        assertThat(connector).isPresent();

        verify(connectorsApiClientMock).createConnector(any());
        verify(connectorsDAOMock).persist(any(ConnectorEntity.class));
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_ACTION_TOPIC, RhoasTopicAccessType.PRODUCER);
    }

    @Test
    @Transactional
    void doNotDeleteConnector() {
        when(rhoasServiceMock.isEnabled()).thenReturn(true);
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(null);

        connectorsService.deleteConnectorIfNeeded(testWebhookAction(), testProcessor(), webhookAction);

        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
        verify(connectorsApiClientMock, never()).deleteConnector(TEST_CONNECTOR_EXTERNAL_ID, KAFKA_ID_IGNORED);
        verify(rhoasServiceMock, never()).deleteTopicAndRevokeAccessFor(TEST_ACTION_TOPIC, RhoasTopicAccessType.PRODUCER);
    }

    @Test
    @Transactional
    void doDeleteConnector() {
        when(rhoasServiceMock.isEnabled()).thenReturn(true);
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(testConnectorEntity());

        connectorsService.deleteConnectorIfNeeded(testKafkaAction(), testProcessor(), slackAction);

        verify(connectorsDAOMock).delete(any(ConnectorEntity.class));
        verify(connectorsApiClientMock).deleteConnector(TEST_CONNECTOR_EXTERNAL_ID, KAFKA_ID_IGNORED);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_ACTION_TOPIC, RhoasTopicAccessType.PRODUCER);
    }

    private Connector testConnector() {
        Connector connector = new Connector();
        connector.setId(TEST_CONNECTOR_ID);
        return connector;
    }

    private ConnectorEntity testConnectorEntity() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(TEST_CONNECTOR_ID);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        connectorEntity.setProcessor(testProcessor());
        return connectorEntity;
    }

    private Processor testProcessor() {
        Processor processor = new Processor();
        processor.setId(TEST_PROCESSOR_ID);
        processor.setName(TEST_PROCESSOR_NAME);
        return processor;
    }

    private BaseAction testKafkaAction() {
        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);
        action.setName(TEST_ACTION_NAME);
        action.setParameters(Map.of(
                SlackAction.CHANNEL_PARAMETER, TEST_ACTION_CHANNEL,
                SlackAction.WEBHOOK_URL_PARAMETER, TEST_ACTION_WEBHOOK,
                KafkaTopicAction.TOPIC_PARAM, TEST_ACTION_TOPIC));
        return action;
    }

    private BaseAction testWebhookAction() {
        BaseAction action = new BaseAction();
        action.setType(WebhookAction.TYPE);
        action.setName(TEST_ACTION_NAME);
        action.setParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }

}
