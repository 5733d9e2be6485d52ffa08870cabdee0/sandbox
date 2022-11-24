package com.redhat.service.smartevents.shard.operator.v2;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;

public interface ManagedBridgeService {

    void createManagedBridge(BridgeDTO bridgeDTO);

    void deleteManagedBridge(BridgeDTO bridgeDTO);
}
