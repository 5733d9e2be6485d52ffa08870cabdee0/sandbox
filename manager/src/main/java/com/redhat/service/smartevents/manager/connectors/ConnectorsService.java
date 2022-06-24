package com.redhat.service.smartevents.manager.connectors;

import com.redhat.service.smartevents.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor);

    void deleteConnectorEntity(Processor processor);

    void updateConnectorEntity(Processor processor);
}
