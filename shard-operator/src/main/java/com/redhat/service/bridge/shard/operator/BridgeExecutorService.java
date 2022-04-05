package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.KnativeTrigger;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeExecutorService {
    void createBridgeExecutor(ProcessorDTO processorDTO);

    void deleteBridgeExecutor(ProcessorDTO processorDTO);

    void createOrUpdateBridgeExecutorSecret(BridgeExecutor bridgeExecutor, ProcessorDTO processorDTO);

    Secret fetchBridgeExecutorSecret(BridgeExecutor bridgeExecutor);

    Deployment fetchOrCreateBridgeExecutorDeployment(BridgeExecutor bridgeExecutor, Secret secret);

    Service fetchOrCreateBridgeExecutorService(BridgeExecutor bridgeExecutor, Deployment deployment);

    KnativeTrigger fetchOrCreateKnativeTrigger(BridgeExecutor bridgeExecutor, Service service);
}
