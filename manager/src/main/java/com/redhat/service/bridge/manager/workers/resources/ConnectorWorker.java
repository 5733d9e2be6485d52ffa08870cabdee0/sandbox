package com.redhat.service.bridge.manager.workers.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.DeploymentLocation;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.openshift.cloud.api.connector.models.ServiceAccount;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/*
    Deploys or deletes a Connector. Connector deployment is asynchronous, so this will poll
    the Connector status on a regular loop until the Connector enters the ready status.
 */
@ApplicationScoped
public class ConnectorWorker extends AbstractWorker<ConnectorEntity> {

    public static final String KAFKA_ID_IGNORED = "kafkaId-ignored";

    @ConfigProperty(name = "managed-connectors.cluster.id")
    String mcClusterId;

    @ConfigProperty(name = "managed-connectors.kafka.bootstrap.servers")
    String kafkaBootstrapServer;

    @ConfigProperty(name = "managed-connectors.kafka.client.id")
    String serviceAccountId;

    @ConfigProperty(name = "managed-connectors.kafka.client.secret")
    String serviceAccountSecret;

    @Inject
    private ConnectorsDAO connectorsDAO;

    @Inject
    private RhoasService rhoasService;

    @Inject
    private ConnectorsApiClient connectorsApi;

    @Override
    PanacheRepositoryBase<ConnectorEntity, String> getDao() {
        return connectorsDAO;
    }

    private void deployConnector(ConnectorEntity connectorEntity) {
        JsonNode payload = connectorEntity.getDefinition();
        String newConnectorName = connectorEntity.getName();
        String connectorType = connectorEntity.getConnectorType();

        ConnectorRequest createConnectorRequest = new ConnectorRequest();

        createConnectorRequest.setName(newConnectorName);

        DeploymentLocation deploymentLocation = new DeploymentLocation();
        deploymentLocation.setKind("addon");
        deploymentLocation.setClusterId(mcClusterId);
        createConnectorRequest.setDeploymentLocation(deploymentLocation);

        createConnectorRequest.setConnectorTypeId(connectorType);

        createConnectorRequest.setConnector(payload);

        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setClientId(serviceAccountId);
        serviceAccount.setClientSecret(serviceAccountSecret);
        createConnectorRequest.setServiceAccount(serviceAccount);

        KafkaConnectionSettings kafka = new KafkaConnectionSettings();
        kafka.setUrl(kafkaBootstrapServer);

        // https://issues.redhat.com/browse/MGDOBR-198
        // this is currently ignored in the Connectors API
        kafka.setId(KAFKA_ID_IGNORED);

        createConnectorRequest.setKafka(kafka);

        Connector c = connectorsApi.createConnector(createConnectorRequest);
        connectorEntity.setConnectorExternalId(c.getId());
    }

    @Override
    void runCreateOfDependencies(ConnectorEntity connectorEntity) {
        //Let's assume this is idempotent
        rhoasService.createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        Connector connector = connectorsApi.getConnector(connectorEntity);
        if (connector != null) {
            /*
                We've already made the request to deploy the connector. What state is it in?
            */
            if (connector.getStatus().getState() == ConnectorState.READY) {
                /*
                    Connector is ready. We can proceed with the deployment of the Processor in the Shard
                 */
                connectorEntity.setStatus(BridgeStatus.READY);
                connectorEntity.getDependencyStatus().setReady(true);
            } else if (connector.getStatus().getState() == ConnectorState.FAILED) {
                /*
                    Deployment of the Connector has failed. Fail the deployment of the Processor
                 */
                connectorEntity.setStatus(BridgeStatus.FAILED);
            }
        } else {
            /*
                There is currently no Connector for the Action of this Processor. Initiate the deployment of the Connector.
             */
            deployConnector(connectorEntity);
        }
    }

    @Override
    void runDeleteOfDependencies(ConnectorEntity connectorEntity) {
        Connector connector = connectorsApi.getConnector(connectorEntity);
        if (connector != null) {
            switch (connector.getStatus().getState()) {
                case DELETED:
                    deleteTopic(connectorEntity);
                    return;
                case FAILED:
                    connectorEntity.setStatus(BridgeStatus.FAILED);
                    return;
                default:
                    connectorsApi.deleteConnector(connectorEntity.getId());
            }
        } else {
            deleteTopic(connectorEntity);
        }
    }

    private void deleteTopic(ConnectorEntity connectorEntity) {
        rhoasService.deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        connectorEntity.getDependencyStatus().setDeleted(true);
    }
}
