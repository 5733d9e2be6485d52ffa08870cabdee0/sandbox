package com.redhat.service.bridge.manager.connectors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.cloud.api.connector.models.Connector;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.actions.connectors.ConnectorsAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class ConnectorsServiceTest {

    @Inject
    ConnectorsService connectorService;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    ConnectorsApi connectorsApi;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Test
    @Transactional
    void doNotCreateConnector() {
        BaseAction action = new BaseAction();
        action.setName("no-mc-action");
        action.setType(WebhookAction.TYPE);

        ProcessorRequest processorRequest = new ProcessorRequest();
        processorRequest.setAction(action);

        Optional<String> connectorId = connectorService.createConnectorIfNeeded(processorRequest, action, null);

        assertThat(connectorId).isEmpty();

        verify(connectorsApi, never()).createConnector(any());
    }

    @Test
    @Transactional
    void createConnector() throws JsonProcessingException {

        Bridge b = Fixtures.createBridge();
        Processor p = Fixtures.createProcessor(b, "foo");

        bridgeDAO.persist(b);
        processorDAO.persist(p);
        String processorId = p.getId();

        BaseAction action = new BaseAction();
        action.setName("mc-action1");
        action.setType(ConnectorsAction.TYPE);

        Map<String, String> actionParameters = new HashMap<>();
        actionParameters.put("connectorType", "slack_sink_0.1");
        actionParameters.put("connectorName", "new-managed-connector"); // TODO-MC topic name will be created from connectorName

        JsonNode connectorPayload = objectMapper.readTree("      { \"connector\": {\n" +
                "         \"channel\": \"channel\", " +
                "         \"webhookUrl\": \"webhook_url\" " +
                "      }, " +
                "      \"kafka\": { " +
                "         \"topic\": \"topic\" " +
                "      } " +
                "}");
        actionParameters.put("connectorPayload", connectorPayload.toString());

        action.setParameters(actionParameters);

        ProcessorRequest processorRequest = new ProcessorRequest();
        processorRequest.setAction(action);

        BaseAction resolvedAction = new BaseAction();
        resolvedAction.setType(KafkaTopicAction.TYPE);
        resolvedAction.getParameters().put(KafkaTopicAction.TOPIC_PARAM, "generatedTopic");

        connectorService.createConnectorIfNeeded(processorRequest, resolvedAction, p);

        ArgumentCaptor<Connector> connectorCaptor = ArgumentCaptor.forClass(Connector.class);
        verify(connectorsApi).createConnector(connectorCaptor.capture());

        Connector calledConnector = connectorCaptor.getValue();

        assertThat(calledConnector.getKafka()).isNotNull();
        assertThat(calledConnector.getKafka().getBootstrapServer()).isEqualTo("localhost:9092");
        assertThat(calledConnector.getKafka().getClientId()).isEqualTo("fake_id");
        assertThat(calledConnector.getKafka().getClientSecret()).isEqualTo("fake_secret");

        ConnectorEntity foundConnector = connectorsDAO.findByProcessorIdAndName(processorId, "new-managed-connector");
        JsonNode definition = foundConnector.getDefinition();
        String kafkaTopic = definition.at("/kafka/topic").asText();
        assertThat(kafkaTopic).isEqualTo("generatedTopic");
    }
}
