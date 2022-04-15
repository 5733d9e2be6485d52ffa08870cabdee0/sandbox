package com.redhat.service.smartevents.manager.connectors;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor, Action action);

    void createConnectorEntity(Processor processor, Source source);

    void deleteConnectorEntity(Processor processor);
}
