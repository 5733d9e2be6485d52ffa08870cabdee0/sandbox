package com.redhat.service.smartevents.manager.connectors;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.processor.actions.slack.SlackConnectorAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;
import com.redhat.service.smartevents.processor.sources.slack.SlackSourceConnector;

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
    private static final String TEST_SOURCE_CHANNEL = "testchannelsrc";
    private static final String TEST_SOURCE_TOKEN = "test-token";

    @Inject
    ConnectorsService connectorsService;

    @InjectMock
    ConnectorsDAO connectorsDAOMock;

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

    private static Stream<Arguments> connectorProcessors() {
        Object[][] arguments = {
                { processorWith(slackAction()), SlackConnectorAction.CONNECTOR_TYPE_ID },
                { processorWith(slackSource()), SlackSourceConnector.CONNECTOR_TYPE_ID }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static ConnectorEntity connectorEntityWith(Processor processor) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setType(processor.getType() == ProcessorType.SOURCE ? ConnectorType.SOURCE : ConnectorType.SINK);
        connectorEntity.setId(TEST_CONNECTOR_ID);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        connectorEntity.setProcessor(processor);
        return connectorEntity;
    }

    private static Processor processorWith(Gateway gateway) {
        Processor processor = new Processor();

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

    private static Action slackAction() {
        Action action = new Action();
        action.setType("slack");
        action.setMapParameters(Map.of(
                "a", TEST_ACTION_CHANNEL,
                "b", TEST_ACTION_WEBHOOK));
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
