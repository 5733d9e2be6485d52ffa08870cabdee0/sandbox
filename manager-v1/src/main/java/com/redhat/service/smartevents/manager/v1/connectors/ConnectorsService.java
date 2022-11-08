package com.redhat.service.smartevents.manager.v1.connectors;

import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor);

    void deleteConnectorEntity(Processor processor);

    void updateConnectorEntity(Processor processor);
}
