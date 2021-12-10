package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.api.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeIngressService {
    void createBridgeIngress(BridgeDTO bridgeDTO);

    void deleteBridgeIngress(BridgeDTO bridgeDTO);

    Deployment fetchOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress);

    Service fetchOrCreateBridgeIngressService(BridgeIngress bridgeIngress, Deployment deployment);
}
