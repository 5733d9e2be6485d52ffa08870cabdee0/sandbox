package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.ServiceComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorClusterIPService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorServiceReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorServiceReconciler.class);

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorClusterIPService bridgeExecutorClusterIPService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        try{
            List<Service> requestResource = createRequiredResources(bridgeExecutor);

            List<Service> deployedResources = fetchDeployedResources(bridgeExecutor);

            processDelta(requestResource, deployedResources);

            statusService.updateStatusForSuccessfulReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.SERVICE_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Bridge Executor Service Monitor", e);
            statusService.updateStatusForFailedReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.SERVICE_AVAILABLE, e);
            throw e;
        }
    }

    private List<Service> createRequiredResources(BridgeExecutor bridgeExecutor) {
        Service requestedService = bridgeExecutorClusterIPService.createBridgeExecutorClusterIPService(bridgeExecutor);
        return Collections.singletonList(requestedService);
    }

    private List<Service> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        Service deployedService = bridgeExecutorClusterIPService.fetchBridgeExecutorClusterIPService(bridgeExecutor);
        return deployedService == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedService);
    }

    private void processDelta(List<Service> requestedResources, List<Service> deployedResources) {
        Comparator<Service> serviceComparator = new ServiceComparator();
        deltaProcessorService.processDelta(Service.class, serviceComparator, requestedResources, deployedResources);
    }
}
