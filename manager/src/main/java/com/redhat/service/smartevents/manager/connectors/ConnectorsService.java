package com.redhat.service.smartevents.manager.connectors;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor, Action action);

    void deleteConnectorEntity(Processor processor);
}
