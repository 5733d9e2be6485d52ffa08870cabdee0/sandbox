package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.bridge.shard.operator.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;

public interface BridgeIngressService {
    void createBridgeIngress(BridgeDTO bridgeDTO);

    void deleteBridgeIngress(BridgeDTO bridgeDTO);

    void createOrUpdateBridgeIngressSecret(BridgeIngress bridgeIngress, BridgeDTO bridgeDTO);

    Secret fetchBridgeIngressSecret(BridgeIngress bridgeIngress);

    ConfigMap fetchOrCreateBridgeIngressConfigMap(BridgeIngress bridgeIngress, Secret secret);

    KnativeBroker fetchOrCreateBridgeIngressBroker(BridgeIngress bridgeIngress, ConfigMap configMap);

    AuthorizationPolicy fetchOrCreateBridgeIngressAuthorizationPolicy(BridgeIngress bridgeIngress);
}
