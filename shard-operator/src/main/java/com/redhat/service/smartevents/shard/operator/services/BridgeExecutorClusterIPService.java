package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.fabric8.kubernetes.api.model.Service;

public interface BridgeExecutorClusterIPService {

    Service createBridgeExecutorClusterIPService(BridgeExecutor bridgeExecutor);

    Service fetchBridgeExecutorClusterIPService(BridgeExecutor bridgeExecutor);
}
