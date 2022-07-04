package com.redhat.service.smartevents.manager.connectors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.processor.GatewayConnector;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private static final String TEST_SOURCE_CHANNEL = "testchannelsrc";
    private static final String TEST_SOURCE_TOKEN = "test-token";

    @Inject
    ConnectorsService connectorsService;

    @InjectMock
    ConnectorsDAO connectorsDAOMock;

    @InjectMock
    GatewayConnector gatewayConnector;

    @InjectMock
    ResourceNamesProvider resourceNamesProvider;

    @Test
    @Transactional
    void doNotCreateConnector() {
        connectorsService.createConnectorEntity(processorWith(webhookAction()));
        verify(connectorsDAOMock, never()).persist(any(ConnectorEntity.class));
    }

    @ParameterizedTest
    @Transactional
    @MethodSource("connectorProcessors")
    void doCreateConnector(Processor processor, String expectedConnectorTypeId) {
        connectorsService.createConnectorEntity(processor);
        ArgumentCaptor<ConnectorEntity> captor = ArgumentCaptor.forClass(ConnectorEntity.class);
        verify(connectorsDAOMock).persist(captor.capture());
        ConnectorEntity entity = captor.getValue();
        assertThat(entity.getConnectorTypeId()).isEqualTo(expectedConnectorTypeId);
    }

    @Transactional
    @Test
    void doCreateMultipleConnector() {
        Action action1 = slackAction("slackAction1");
        Action action2 = slackAction("slackAction2");
        Processor processor = processorWithMultipleActions(action1, action2);

        connectorsService.createConnectorEntity(processor);

        ArgumentCaptor<ConnectorEntity> captor = ArgumentCaptor.forClass(ConnectorEntity.class);
        verify(connectorsDAOMock, times(2)).persist(captor.capture());

        List<ConnectorEntity> entities = captor.getAllValues();
        assertThat(entities).map(ConnectorEntity::getConnectorTypeId).containsExactlyInAnyOrder(SlackAction.TYPE, SlackAction.TYPE);
    }

    @Test
    @Transactional
    void doNotDeleteConnector() {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(null);
        connectorsService.deleteConnectorEntity(processorWith(webhookAction()));
        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
    }

    @ParameterizedTest
    @Transactional
    @MethodSource("connectorProcessors")
    void doDeleteConnector(Processor processor) {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(connectorEntityWith(processor));
        connectorsService.deleteConnectorEntity(processor);
        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
    }

    @ParameterizedTest
    @Transactional
    @MethodSource("connectorProcessors")
    void doUpdateConnectorNoneFound(Processor processor) {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(null);

        connectorsService.updateConnectorEntity(processor);

        verify(gatewayConnector, never()).connectorPayload(any(), any(), any());
    }

    @ParameterizedTest
    @Transactional
    @MethodSource("connectorProcessors")
    void doUpdateConnector(Processor processor) {
        ConnectorEntity connectorEntity = connectorEntityWith(processor);
        // We need to ensure the Processor looks as though it has been updated
        processor.setGeneration(1);
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(connectorEntity);
        when(resourceNamesProvider.getBridgeErrorTopicName(processor.getBridge().getId())).thenReturn("TopicNameError");
        when(gatewayConnector.connectorPayload(any(), any(), any())).thenReturn(new TextNode("definition-updated"));

        connectorsService.updateConnectorEntity(processor);

        if (processor.getType() == ProcessorType.SOURCE) {
            verify(gatewayConnector).connectorPayload(processor.getDefinition().getRequestedSource(), connectorEntity.getTopicName(), "TopicNameError");
        } else if (processor.getType() == ProcessorType.SINK) {
            verify(gatewayConnector).connectorPayload(processor.getDefinition().getRequestedAction(), connectorEntity.getTopicName(), "TopicNameError");
        }

        assertThat(connectorEntity.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(connectorEntity.getDependencyStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
    }

    private static Stream<Arguments> connectorProcessors() {
        Object[][] arguments = {
                { processorWith(slackAction("actionName")), SlackAction.TYPE },
                { processorWith(slackSource()), SlackSource.TYPE }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static ConnectorEntity connectorEntityWith(Processor processor) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setType(processor.getType() == ProcessorType.SOURCE ? ConnectorType.SOURCE : ConnectorType.SINK);
        connectorEntity.setId(TEST_CONNECTOR_ID);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        connectorEntity.setProcessor(processor);
        connectorEntity.setDefinition(new TextNode("definition"));
        return connectorEntity;
    }

    private static Processor processorWith(Gateway gateway) {
        Processor processor = new Processor();
        processor.setBridge(new Bridge());

        ProcessorDefinition processorDefinition = new ProcessorDefinition();
        if (gateway instanceof Action) {
            processorDefinition.setRequestedAction((Action) gateway);
            processor.setType(ProcessorType.SINK);
        } else {
            processorDefinition.setRequestedSource((Source) gateway);
            processor.setType(ProcessorType.SOURCE);
        }

        processor.setId(TEST_PROCESSOR_ID);
        processor.setName(TEST_PROCESSOR_NAME);
        processor.setDefinition(processorDefinition);
        return processor;
    }

    private static Processor processorWithMultipleActions(Action... actions) {
        Processor processor = new Processor();
        processor.setBridge(new Bridge());

        ProcessorDefinition processorDefinition = new ProcessorDefinition();
        processor.setType(ProcessorType.SINK);

        processorDefinition.setRequestedActions(Arrays.asList(actions));
        processorDefinition.setResolvedActions(Arrays.asList(actions));

        processor.setId(TEST_PROCESSOR_ID);
        processor.setName(TEST_PROCESSOR_NAME);
        processor.setDefinition(processorDefinition);
        return processor;
    }

    private static Action slackAction(String actionName) {
        Action action = new Action();
        action.setName(actionName);
        action.setType(SlackAction.TYPE);
        action.setMapParameters(Map.of(
                SlackAction.CHANNEL_PARAM, TEST_ACTION_CHANNEL,
                SlackAction.WEBHOOK_URL_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }

    private static Source slackSource() {
        Source action = new Source();
        action.setType(SlackSource.TYPE);
        action.setMapParameters(Map.of(
                SlackSource.CHANNEL_PARAM, TEST_SOURCE_CHANNEL,
                SlackSource.TOKEN_PARAM, TEST_SOURCE_TOKEN));
        return action;
    }

    private static Action webhookAction() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }
}
