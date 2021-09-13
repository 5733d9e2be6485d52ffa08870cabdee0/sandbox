package com.redhat.service.bridge.shard;

import com.redhat.service.bridge.infra.dto.BridgeDTO;

public interface OperatorService {
    BridgeDTO createBridgeDeployment(BridgeDTO bridge);

    BridgeDTO deleteBridgeDeployment(BridgeDTO bridge);
}
