package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public interface BridgeExecutorServiceMonitorService {

    ServiceMonitor createBridgeExecutorServiceMonitorService(BridgeExecutor bridgeExecutor);

    ServiceMonitor fetchBridgeExecutorServiceMonitorService(BridgeExecutor bridgeExecutor);
}
