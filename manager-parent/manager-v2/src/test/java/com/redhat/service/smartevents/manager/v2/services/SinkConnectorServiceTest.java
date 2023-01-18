package com.redhat.service.smartevents.manager.v2.services;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.models.connectors.ConnectorDefinition;
import com.redhat.service.smartevents.infra.v2.api.models.dto.SinkConnectorDTO;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SinkConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnector;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnectorReadyConditions;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class SinkConnectorServiceTest extends AbstractConnectorServiceTest {

    @ConfigProperty(name = "event-bridge.sink-connectors.deployment.timeout-seconds")
    int managedSinkConnectorsTimeoutSeconds;

    @Inject
    SinkConnectorService sinkConnectorService;

    @Inject
    SinkConnectorDAO sinkConnectorDAO;

    @Override
    public ConnectorService getConnectorService() {
        return sinkConnectorService;
    }

    @Override
    public ConnectorDAO getConnectorDAO() {
        return sinkConnectorDAO;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SINK;
    }

    @Override
    public void additionalResponseAssertions(ConnectorResponse connectorResponse, Bridge bridge, Connector connector) {
        assertThat(connectorResponse).isInstanceOf(SinkConnectorResponse.class);
        assertThat(((SinkConnectorResponse) connectorResponse).getUriDsl()).startsWith("knative").contains(connectorResponse.getId());
    }

    @Test
    public void testToDTO() {
        Bridge bridge = createBridge();
        Connector connector = createConnector(bridge, getConnectorType());
        connector.setOwner("Owner");
        connector.setConditions(createConnectorReadyConditions());
        connector.setDefinition(new ConnectorDefinition(JsonNodeFactory.instance.objectNode()));

        SinkConnectorDTO dto = sinkConnectorService.toDTO(connector);

        assertThat(dto.getId()).isEqualTo(connector.getId());
        assertThat(dto.getBridgeId()).isEqualTo(connector.getBridge().getId());
        assertThat(dto.getCustomerId()).isEqualTo(connector.getBridge().getCustomerId());
        assertThat(dto.getOwner()).isEqualTo("Owner");
        assertThat(dto.getName()).isEqualTo(connector.getName());
        assertThat(dto.getOperationType()).isEqualTo(connector.getOperation().getType());
        assertThat(dto.getGeneration()).isEqualTo(connector.getGeneration());
        assertThat(dto.getTimeoutSeconds()).isEqualTo(managedSinkConnectorsTimeoutSeconds);
        assertThat(dto.getKafkaConnection()).isNull(); // TODO: kafka connections will be implemented with https://issues.redhat.com/browse/MGDOBR-1411
    }
}
