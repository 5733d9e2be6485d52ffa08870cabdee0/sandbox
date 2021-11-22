package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeExecutorService {
    void createBridgeExecutor(ProcessorDTO processorDTO);

    void deleteBridgeExecutor(ProcessorDTO processorDTO);

    ConfigMap fetchOrCreateBridgeExecutorProcessorConfigMap(BridgeExecutor bridgeExecutor);

    Deployment fetchOrCreateBridgeExecutorDeployment(BridgeExecutor bridgeExecutor, ConfigMap processorConfigMap);

    Service fetchOrCreateBridgeExecutorService(BridgeExecutor bridgeExecutor, Deployment deployment);
}
