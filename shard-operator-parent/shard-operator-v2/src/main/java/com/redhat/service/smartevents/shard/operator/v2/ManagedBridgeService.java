package com.redhat.service.smartevents.shard.operator.v2;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;

public interface ManagedBridgeService {

    void createManagedBridge(BridgeDTO bridgeDTO);

    void deleteManagedBridge(BridgeDTO bridgeDTO);

    Secret fetchOrCreateBridgeSecret(ManagedBridge managedBridge);

    ConfigMap fetchOrCreateBridgeConfigMap(ManagedBridge managedBridge, Secret secret);

    KnativeBroker fetchOrCreateKnativeBroker(ManagedBridge managedBridge, ConfigMap configMap);
}
