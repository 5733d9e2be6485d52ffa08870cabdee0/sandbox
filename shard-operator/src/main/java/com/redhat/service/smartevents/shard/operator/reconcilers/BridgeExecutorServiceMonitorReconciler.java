package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.ServiceMonitorComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorServiceMonitorReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorServiceMonitorReconciler.class);

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorServiceMonitorService bridgeExecutorServiceMonitorService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        try{
            List<ServiceMonitor> requestResource = createRequiredResources(bridgeExecutor);

            List<ServiceMonitor> deployedResources = fetchDeployedResources(bridgeExecutor);

            processDelta(requestResource, deployedResources);

            statusService.updateStatusForSuccessfulReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.SERVICE_MONITOR_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Bridge Executor Service Monitor", e);
            statusService.updateStatusForFailedReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.SERVICE_MONITOR_AVAILABLE, e);
            throw e;
        }
    }

    private List<ServiceMonitor> createRequiredResources(BridgeExecutor bridgeExecutor) {
        ServiceMonitor requestedServiceMonitor = bridgeExecutorServiceMonitorService.createBridgeExecutorServiceMonitorService(bridgeExecutor);
        return Collections.singletonList(requestedServiceMonitor);
    }

    private List<ServiceMonitor> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        ServiceMonitor deployedServiceMonitor = bridgeExecutorServiceMonitorService.createBridgeExecutorServiceMonitorService(bridgeExecutor);
        return deployedServiceMonitor == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedServiceMonitor);
    }

    private void processDelta(List<ServiceMonitor> requestedResources, List<ServiceMonitor> deployedResources) {
        Comparator<ServiceMonitor> serviceMonitorComparator = new ServiceMonitorComparator();
        deltaProcessorService.processDelta(ServiceMonitor.class, serviceMonitorComparator, requestedResources, deployedResources);
    }
}
