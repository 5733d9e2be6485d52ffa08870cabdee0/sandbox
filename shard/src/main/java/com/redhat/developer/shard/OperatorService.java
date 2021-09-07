package com.redhat.developer.shard;

import com.redhat.developer.infra.dto.BridgeDTO;

public interface OperatorService {
    BridgeDTO createBridgeDeployment(BridgeDTO bridge);
}
