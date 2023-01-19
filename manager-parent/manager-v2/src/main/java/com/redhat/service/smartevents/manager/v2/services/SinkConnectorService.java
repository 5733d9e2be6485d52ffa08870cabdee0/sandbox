package com.redhat.service.smartevents.manager.v2.services;

import com.redhat.service.smartevents.infra.v2.api.models.dto.SinkConnectorDTO;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;

public interface SinkConnectorService extends ConnectorService<SinkConnectorResponse>,
        ShardManagedResourceService<Connector, SinkConnectorDTO> {

}
