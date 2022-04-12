package com.redhat.service.smartevents.manager.connectors;

import com.redhat.service.smartevents.infra.models.actions.BaseAction;
import com.redhat.service.smartevents.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor, BaseAction action);

    void deleteConnectorEntity(Processor processor);
}
