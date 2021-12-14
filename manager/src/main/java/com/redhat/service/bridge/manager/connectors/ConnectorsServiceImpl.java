package com.redhat.service.bridge.manager.connectors;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openshift.cloud.api.connector.models.AddonClusterTarget;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorAllOfMetadata;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.actions.connectors.ConnectorsAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;

@ApplicationScoped
public class ConnectorsServiceImpl implements ConnectorsService {

    @Inject
    ObjectMapper mapper;

    @Inject
    ConnectorsApi connectorsApi;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ProcessorDAO processorDAO;

    @ConfigProperty(name = "managed-connectors.cluster.id")
    String mcClusterId;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafkaBootstrapServer;

    @ConfigProperty(name = "kafka.client.id")
    String serviceAccountId;

    @ConfigProperty(name = "kafka.client.secret")
    String serviceAccountSecret;

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<String> createConnectorIfNeeded(ProcessorRequest processorRequest,
            BaseAction resolvedAction,
            Processor processor) {

        BaseAction action = processorRequest.getAction();

        if (!ConnectorsAction.TYPE.equals(action.getType())) {
            return Optional.empty();
        }

        Map<String, String> actionParameters = action.getParameters();
        String connectorType = actionParameters.get("connectorType");
        String newConnectorName = actionParameters.get("connectorName");

        JsonNode connectorPayload = parseConnectorPayloadString(actionParameters);

        // Assume connectors actions are transformed to send-to-kafka
        String kafkaTopicName = resolvedAction.getParameters().get(KafkaTopicAction.TOPIC_PARAM);
        if (kafkaTopicName == null) {
            throw new RuntimeException("Need a kafka topic to create a connector");
        }
        updateKafkaTopicInConnectorPayload(connectorPayload, kafkaTopicName);

        persistConnector(processor, newConnectorName, connectorPayload);

        try {
            Connector connector = callConnectorService(connectorType, connectorPayload, newConnectorName);
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO-MC error handling
        }

        return Optional.of(newConnectorName);
    }

    private void updateKafkaTopicInConnectorPayload(JsonNode connectorPayload, String kafkaTopicName) {
        // Assume the Connector action is always transformed to a send-to-kafka Action
        String kafkaTopic = kafkaTopicName;
        ObjectNode kafka = connectorPayload.with("kafka");
        kafka.put("topic", kafkaTopic);
    }

    private void persistConnector(Processor processor, String newConnectorName, JsonNode connectorPayload) {
        ConnectorEntity newConnectorEntity = new ConnectorEntity();

        newConnectorEntity.setName(newConnectorName);
        newConnectorEntity.setStatus(ConnectorStatus.REQUESTED);
        newConnectorEntity.setSubmittedAt(ZonedDateTime.now());
        newConnectorEntity.setPublishedAt(ZonedDateTime.now());
        newConnectorEntity.setProcessor(processor);
        newConnectorEntity.setDefinition(connectorPayload);

        connectorsDAO.persist(newConnectorEntity);
    }

    private JsonNode parseConnectorPayloadString(Map<String, String> actionParameters) {
        try {
            String connectorPayloadString = actionParameters.get("connectorPayload");
            return mapper.readTree(connectorPayloadString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e); // TODO-MC error handling
        }
    }

    private Connector callConnectorService(String connectorType,
            JsonNode connectorPayload,
            String newConnectorName) {
        Connector createConnectorRequest = new Connector();

        ConnectorAllOfMetadata metadata = new ConnectorAllOfMetadata();
        metadata.setName(newConnectorName);
        metadata.setKafkaId("kafkaId-ignored"); // this is currently ignored in the Connectors API
        createConnectorRequest.setMetadata(metadata);

        AddonClusterTarget deploymentLocation = new AddonClusterTarget();
        deploymentLocation.setKind("addon");
        deploymentLocation.setClusterId(mcClusterId);
        createConnectorRequest.setDeploymentLocation(deploymentLocation);

        createConnectorRequest.setConnectorTypeId(connectorType);

        createConnectorRequest.setConnectorSpec(connectorPayload);

        KafkaConnectionSettings kafka = new KafkaConnectionSettings();
        kafka.setBootstrapServer(kafkaBootstrapServer);
        kafka.setClientId(serviceAccountId);
        kafka.setClientSecret(serviceAccountSecret);
        createConnectorRequest.setKafka(kafka);

        try {
            Connector connectorResult = connectorsApi.createConnector(createConnectorRequest);
            return connectorResult;
        } catch (WebApplicationException e) {
            errorLogging(e);
            throw e;
        }
    }

    // TODO-MC better error logging
    private void errorLogging(WebApplicationException e) {
        Response response = e.getResponse();
        System.out.println("Error code: " + response.getStatus());

        ByteArrayInputStream arrayInputStream = (ByteArrayInputStream) response.getEntity();

        Scanner scanner = new Scanner(arrayInputStream);
        scanner.useDelimiter("\\Z");//To read all scanner content in one String
        String data = "";
        if (scanner.hasNext()) {
            data = scanner.next();
        }
        System.out.println(data);
    }
}
