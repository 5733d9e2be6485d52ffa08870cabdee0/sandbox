package com.redhat.service.bridge.manager.connectors.creation;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.openshift.cloud.api.connector.models.Connector;
import com.redhat.service.bridge.infra.exceptions.BridgeErrorDAO;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorNotFoundException;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.connectors.AbstractConnectorWorker;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.connectors.Events;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class CheckConnectorAvailableWorker extends AbstractConnectorWorker<Connector> {

    @Inject
    ConnectorsApiClient connectorsApiClient;

    @Inject
    BridgeErrorDAO bridgeErrors;

    @ConsumeEvent(value = Events.CONNECTOR_MANAGED_CONNECTOR_CREATED_EVENT, blocking = true)
    public void consume(ConnectorEntity connectorEntity) {
        execute(connectorEntity);
    }

    @Override
    protected Connector callExternalService(ConnectorEntity connectorEntity) {
        Connector connector = connectorsApiClient.getConnector(connectorEntity);

        if (Objects.isNull(connector)) {
            throw new ConnectorNotFoundException(bridgeErrors.findByException(ConnectorNotFoundException.class).getReason());
        }

        return connector;
    }

    @Override
    protected ConnectorEntity updateEntityForSuccess(ConnectorEntity connectorEntity, Connector connector) {
        connectorEntity.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        connectorEntity.setStatus(ConnectorStatus.READY);
        return connectorEntity;
    }

    @Override
    protected ConnectorEntity updateEntityForError(ConnectorEntity connectorEntity, Throwable error) {
        //The Managed Connector may not have been provisioned so don't fail the lookup operation.
        connectorEntity.setStatus(ConnectorStatus.MANAGED_CONNECTOR_LOOKUP_FAILED);
        return connectorEntity;
    }

}
