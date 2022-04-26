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

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionConnector;
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
        connectorsService.createConnectorEntity(testProcessor(testWebhookAction()));
        verify(connectorsDAOMock, never()).persist(any(ConnectorEntity.class));
    }

    @ParameterizedTest
    @Transactional
    @MethodSource("connectorProcessors")
    void doCreateConnector(Processor processor, String expectedConnectorType) {
        connectorsService.createConnectorEntity(processor);
        ArgumentCaptor<ConnectorEntity> captor = ArgumentCaptor.forClass(ConnectorEntity.class);
        verify(connectorsDAOMock).persist(captor.capture());
        ConnectorEntity entity = captor.getValue();
        assertThat(entity.getConnectorType()).isEqualTo(expectedConnectorType);
    }

    @Test
    @Transactional
    void doNotDeleteConnector() {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(null);
        connectorsService.deleteConnectorEntity(testProcessor(testWebhookAction()));
        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
    }

    @ParameterizedTest
    @Transactional
    @MethodSource("connectorProcessors")
    void doDeleteConnector(Processor processor) {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(testConnectorEntity(processor));
        connectorsService.deleteConnectorEntity(processor);
        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
    }

    private static Stream<Arguments> connectorProcessors() {
        Object[][] arguments = {
                { testProcessor(testSlackAction()), SlackActionConnector.CONNECTOR_TYPE },
                { testProcessor(testSlackSource()), SlackSourceConnector.CONNECTOR_TYPE }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static ConnectorEntity testConnectorEntity(Processor processor) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(TEST_CONNECTOR_ID);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        connectorEntity.setProcessor(processor);
        return connectorEntity;
    }

    private static Processor testProcessor(Gateway gateway) {
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

    private static Action testSlackAction() {
        Action action = new Action();
        action.setType(SlackAction.TYPE);
        action.setParameters(Map.of(
                SlackAction.CHANNEL_PARAM, TEST_ACTION_CHANNEL,
                SlackAction.WEBHOOK_URL_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }

    private static Source testSlackSource() {
        Source action = new Source();
        action.setType(SlackSource.TYPE);
        action.setParameters(Map.of(
                SlackSource.CHANNEL_PARAM, TEST_SOURCE_CHANNEL,
                SlackSource.TOKEN_PARAM, TEST_SOURCE_TOKEN));
        return action;
    }

    private Action testWebhookAction() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }
}
