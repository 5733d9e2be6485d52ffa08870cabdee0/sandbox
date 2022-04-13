package com.redhat.service.smartevents.manager.connectors;

import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.actions.Source;
import com.redhat.service.smartevents.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor, Action action);

    void createConnectorEntity(Processor processor, Source source);

    void deleteConnectorEntity(Processor processor);
}
