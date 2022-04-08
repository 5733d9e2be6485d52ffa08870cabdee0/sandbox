package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeIngressService {
    void createBridgeIngress(BridgeDTO bridgeDTO);

    void deleteBridgeIngress(BridgeDTO bridgeDTO);

    void createOrUpdateBridgeIngressSecret(BridgeIngress bridgeIngress, BridgeDTO bridgeDTO);

    Secret fetchBridgeIngressSecret(BridgeIngress bridgeIngress);

    Deployment fetchOrCreateBridgeIngressDeployment(BridgeIngress bridgeIngress, Secret secret);

    Service fetchOrCreateBridgeIngressService(BridgeIngress bridgeIngress, Deployment deployment);
}
