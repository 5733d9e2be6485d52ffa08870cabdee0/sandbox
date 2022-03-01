package com.redhat.service.bridge.manager.connectors;

import java.util.ArrayList;
import java.util.List;
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
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

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
    private static final String TEST_ACTION_CHANNEL = "testchannel";
    private static final String TEST_ACTION_WEBHOOK = "https://test.example.com/webhook";

    private static final String TEST_ACTION_TOPIC = "ob-" + TEST_PROCESSOR_ID;

    @Inject
    SlackAction slackAction;

    @Inject
    WebhookAction webhookAction;

    @Inject
    ConnectorsService connectorsService;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @InjectMock
    ConnectorsApiClient connectorsApiClientMock;

    @InjectMock
    ConnectorsDAO connectorsDAOMock;

    @InjectMock
    RhoasService rhoasServiceMock;

    @Test
    @Transactional
    void doNotCreateConnector() {
        Optional<ConnectorEntity> connector = connectorsService.createConnectorEntity(testWebhookAction(), testProcessor(), webhookAction);
        assertThat(connector).isEmpty();

        verify(connectorsApiClientMock, never()).createConnector(any());
        verify(connectorsDAOMock, never()).persist(any(ConnectorEntity.class));
        verify(rhoasServiceMock, never()).createTopicAndGrantAccessFor(any(), any());
    }

    @Test
    @Transactional
    void doCreateConnector() {
        when(connectorsApiClientMock.createConnector(any())).thenReturn(testConnector());

        Optional<ConnectorEntity> connector = connectorsService.createConnectorEntity(testKafkaAction(), testProcessor(), slackAction);
        assertThat(connector).isPresent();

        verify(connectorsApiClientMock, never()).createConnector(any());
        verify(connectorsDAOMock).persist(any(ConnectorEntity.class));
        verify(rhoasServiceMock, never()).createTopicAndGrantAccessFor(TEST_ACTION_TOPIC, RhoasTopicAccessType.PRODUCER);
    }

    @Test
    @Transactional
    void doNotDeleteConnector() {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(null);
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(new ArrayList<>());

        connectorsService.deleteConnectorIfNeeded(testProcessor());

        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
        verify(connectorsApiClientMock, never()).deleteConnector(TEST_CONNECTOR_EXTERNAL_ID);
        verify(rhoasServiceMock, never()).deleteTopicAndRevokeAccessFor(testActionTopic(), RhoasTopicAccessType.PRODUCER);
    }

    @Test
    @Transactional
    void doDeleteConnector() {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(List.of(testConnectorEntity()));

        connectorsService.deleteConnectorIfNeeded(testProcessor());

        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
        verify(connectorsApiClientMock, never()).deleteConnector(TEST_CONNECTOR_EXTERNAL_ID);
        verify(rhoasServiceMock, never()).deleteTopicAndRevokeAccessFor(TEST_ACTION_TOPIC, RhoasTopicAccessType.PRODUCER);
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
        action.setParameters(Map.of(
                SlackAction.CHANNEL_PARAMETER, TEST_ACTION_CHANNEL,
                SlackAction.WEBHOOK_URL_PARAMETER, TEST_ACTION_WEBHOOK,
                KafkaTopicAction.TOPIC_PARAM, testActionTopic()));
        return action;
    }

    private BaseAction testWebhookAction() {
        BaseAction action = new BaseAction();
        action.setType(WebhookAction.TYPE);
        action.setParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }

    private String testActionTopic() {
        return internalKafkaConfigurationProvider.getTopicPrefix() + TEST_PROCESSOR_ID;
    }

}
