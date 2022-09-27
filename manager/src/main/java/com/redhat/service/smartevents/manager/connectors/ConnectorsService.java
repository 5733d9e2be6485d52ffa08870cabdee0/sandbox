package com.redhat.service.smartevents.manager.connectors;

import com.redhat.service.smartevents.manager.persistence.v1.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor);

    void deleteConnectorEntity(Processor processor);

    void updateConnectorEntity(Processor processor);
}
