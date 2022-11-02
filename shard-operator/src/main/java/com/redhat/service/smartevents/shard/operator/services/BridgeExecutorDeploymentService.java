package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface BridgeExecutorDeploymentService {

    Deployment createBridgeExecutorDeployment(BridgeExecutor bridgeExecutor);

    Deployment fetchBridgeExecutorDeployment(BridgeExecutor bridgeExecutor);
}
