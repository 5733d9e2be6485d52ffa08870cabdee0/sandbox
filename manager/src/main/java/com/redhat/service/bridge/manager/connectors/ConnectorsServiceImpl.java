package com.redhat.service.bridge.manager.connectors;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.AddonClusterTarget;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorAllOfMetadata;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.actions.connectors.ConnectorAction;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;

@ApplicationScoped
public class ConnectorsServiceImpl implements ConnectorsService {

    public static final String KAFKA_ID_IGNORED = "kafkaId-ignored";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsServiceImpl.class);

    @Inject
    ConnectorsApiClient connectorsApiClient;

    @Inject
    ConnectorsDAO connectorsDAO;

    @ConfigProperty(name = "managed-connectors.cluster.id")
    String mcClusterId;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafkaBootstrapServer;

    @ConfigProperty(name = "kafka.client.id")
    String serviceAccountId;

    @ConfigProperty(name = "kafka.client.secret")
    String serviceAccountSecret;

    @Override
    @Transactional
    public Optional<ConnectorEntity> createConnectorIfNeeded(BaseAction resolvedAction,
            Processor processor,
            ActionProvider actionProvider) {

        if (!actionProvider.isConnectorAction()) {
            return Optional.empty();
        }

        ConnectorAction connectorAction = (ConnectorAction) actionProvider;
        JsonNode connectorPayload = connectorAction.connectorPayload(resolvedAction);

        String connectorType = connectorAction.getConnectorType();
        String newConnectorName = connectorName(connectorType, processor);

        ConnectorEntity newConnectorEntity = persistConnector(processor, newConnectorName, connectorPayload);

        Connector connector = callConnectorService(connectorType, connectorPayload, newConnectorName);

        newConnectorEntity.setConnectorExternalId(connector.getId());

        return Optional.of(newConnectorEntity);
    }

    @Override
    public void deleteConnectorIfNeeded(Processor processor) {

        List<ConnectorEntity> connectors = connectorsDAO.findByProcessorId(processor.getId());

        for (ConnectorEntity c : connectors) {
            String connectorExternalId = c.getConnectorExternalId();
            String connectorId = c.getId();
            connectorsDAO.delete(c);
            LOGGER.info("[manager] connector with id '{}' has been deleted", connectorId);

            connectorsApiClient.deleteConnector(connectorExternalId, KAFKA_ID_IGNORED);
        }
    }

    private String connectorName(String connectorType, Processor processor) {
        return String.format("OpenBridge-%s-%s", connectorType, processor.getId());
    }

    private ConnectorEntity persistConnector(Processor processor, String newConnectorName, JsonNode connectorPayload) {
        ConnectorEntity newConnectorEntity = new ConnectorEntity();

        newConnectorEntity.setName(newConnectorName);
        newConnectorEntity.setStatus(ConnectorStatus.REQUESTED);
        newConnectorEntity.setSubmittedAt(ZonedDateTime.now());
        newConnectorEntity.setPublishedAt(ZonedDateTime.now());
        newConnectorEntity.setProcessor(processor);
        newConnectorEntity.setDefinition(connectorPayload);

        connectorsDAO.persist(newConnectorEntity);

        return newConnectorEntity;
    }

    private Connector callConnectorService(
            String connectorType,
            JsonNode connectorPayload,
            String newConnectorName) {
        Connector createConnectorRequest = new Connector();

        ConnectorAllOfMetadata metadata = new ConnectorAllOfMetadata();
        metadata.setName(newConnectorName);
        // https://issues.redhat.com/browse/MGDOBR-198
        metadata.setKafkaId(KAFKA_ID_IGNORED); // this is currently ignored in the Connectors API
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

        return connectorsApiClient.createConnector(createConnectorRequest);
    }
}
