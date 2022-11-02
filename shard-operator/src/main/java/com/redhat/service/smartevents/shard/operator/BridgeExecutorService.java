package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;

public interface BridgeExecutorService {
    void createBridgeExecutor(ProcessorDTO processorDTO);

    void deleteBridgeExecutor(ProcessorDTO processorDTO);

    void createOrUpdateBridgeExecutorSecret(BridgeExecutor bridgeExecutor, ProcessorDTO processorDTO);

    Secret fetchBridgeExecutorSecret(BridgeExecutor bridgeExecutor);

    Deployment fetchOrCreateBridgeExecutorDeployment(BridgeExecutor bridgeExecutor, Secret secret);

    Service fetchOrCreateBridgeExecutorService(BridgeExecutor bridgeExecutor, Deployment deployment);

    String getExecutorImage();

    List<BridgeExecutor> fetchAllBridgeExecutor();
}
