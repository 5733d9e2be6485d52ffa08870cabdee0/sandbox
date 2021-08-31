package com.redhat.developer.shard;

import com.redhat.developer.infra.dto.ConnectorDTO;

public interface OperatorService {
    ConnectorDTO createConnectorDeployment(ConnectorDTO connector);
}
