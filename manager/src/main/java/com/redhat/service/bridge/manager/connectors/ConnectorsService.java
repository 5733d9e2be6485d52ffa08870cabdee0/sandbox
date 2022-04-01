package com.redhat.service.bridge.manager.connectors;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor, BaseAction resolvedAction);

    void deleteConnectorEntity(Processor processor);
}
