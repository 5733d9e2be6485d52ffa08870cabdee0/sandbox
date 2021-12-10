package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.api.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeExecutorService {
    void createBridgeExecutor(ProcessorDTO processorDTO);

    void deleteBridgeExecutor(ProcessorDTO processorDTO);

    Deployment fetchOrCreateBridgeExecutorDeployment(BridgeExecutor bridgeExecutor);

    Service fetchOrCreateBridgeExecutorService(BridgeExecutor bridgeExecutor, Deployment deployment);
}
