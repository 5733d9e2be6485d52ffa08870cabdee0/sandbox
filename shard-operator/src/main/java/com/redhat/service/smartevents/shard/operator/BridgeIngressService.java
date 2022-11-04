package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.infra.api.v1.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;

public interface BridgeIngressService {
    void createBridgeIngress(BridgeDTO bridgeDTO);

    void deleteBridgeIngress(BridgeDTO bridgeDTO);

    void createOrUpdateBridgeIngressSecret(BridgeIngress bridgeIngress, BridgeDTO bridgeDTO);

    Secret fetchBridgeIngressSecret(BridgeIngress bridgeIngress);

    ConfigMap fetchOrCreateBridgeIngressConfigMap(BridgeIngress bridgeIngress, Secret secret);

    KnativeBroker fetchOrCreateBridgeIngressBroker(BridgeIngress bridgeIngress, ConfigMap configMap);

    AuthorizationPolicy fetchOrCreateBridgeIngressAuthorizationPolicy(BridgeIngress bridgeIngress, String path);
}
