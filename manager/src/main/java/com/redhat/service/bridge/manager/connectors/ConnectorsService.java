package com.redhat.service.bridge.manager.connectors;

import java.util.List;
import java.util.Optional;

import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;

public interface ConnectorsService {

    Optional<ConnectorEntity> createConnectorEntity(BaseAction resolvedAction,
            Processor processor,
            ActionProvider actionProvider);

    List<ConnectorEntity> deleteConnectorIfNeeded(Processor processor);
}
