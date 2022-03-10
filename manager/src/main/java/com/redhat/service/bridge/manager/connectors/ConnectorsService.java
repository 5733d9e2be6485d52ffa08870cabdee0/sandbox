package com.redhat.service.bridge.manager.connectors;

import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(BaseAction resolvedAction,
            Processor processor,
            ActionProvider actionProvider);

    void deleteConnectorEntity(Processor processor);
}
