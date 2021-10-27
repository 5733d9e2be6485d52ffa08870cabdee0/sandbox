package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeIngressService {
    void createBridgeIngress(BridgeDTO bridgeDTO);

    void deleteBridgeIngress(BridgeDTO bridgeDTO);

    Deployment getOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress);
}
