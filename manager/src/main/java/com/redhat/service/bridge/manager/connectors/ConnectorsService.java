package com.redhat.service.bridge.manager.connectors;

import java.util.Optional;

import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;

public interface ConnectorsService {

    Optional<ConnectorEntity> createConnectorIfNeeded(BaseAction resolvedAction,
            Processor processor,
            ActionProvider actionProvider);

    void deleteConnectorIfNeeded(Processor processor);
}
