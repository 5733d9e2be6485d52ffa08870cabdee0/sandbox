package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;

public interface BridgeIngressService {
    void createBridgeIngress(BridgeDTO bridgeDTO);

    void deleteBridgeIngress(BridgeDTO bridgeDTO);
}
