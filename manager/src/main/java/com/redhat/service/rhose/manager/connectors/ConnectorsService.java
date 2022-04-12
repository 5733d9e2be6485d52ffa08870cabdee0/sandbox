package com.redhat.service.rhose.manager.connectors;

import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor, BaseAction action);

    void deleteConnectorEntity(Processor processor);
}
