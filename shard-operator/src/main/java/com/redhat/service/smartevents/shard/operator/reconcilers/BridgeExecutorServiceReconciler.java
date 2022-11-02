package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.ServiceComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorClusterIPService;
import io.fabric8.kubernetes.api.model.Service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorServiceReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorClusterIPService bridgeExecutorClusterIPService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        List<Service> requestResource = createRequiredResources(bridgeExecutor);

        List<Service> deployedResources = fetchDeployedResources(bridgeExecutor);

        processDelta(requestResource, deployedResources);

        /*if (service.getStatus() == null) {
            LOGGER.info("Executor service BridgeProcessor: '{}' in namespace '{}' is NOT ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeExecutorStatus.SERVICE_AVAILABLE)) {
                status.markConditionFalse(BridgeExecutorStatus.SERVICE_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeExecutorStatus.SERVICE_AVAILABLE)) {
            status.markConditionTrue(BridgeExecutorStatus.SERVICE_AVAILABLE);
        }*/
    }

    private List<Service> createRequiredResources(BridgeExecutor bridgeExecutor) {
        Service requestedKafkaSecret = bridgeExecutorClusterIPService.createBridgeExecutorClusterIPService(bridgeExecutor);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<Service> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        Service deployedKafkaSecret = bridgeExecutorClusterIPService.fetchBridgeExecutorClusterIPService(bridgeExecutor);
        return Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<Service> requestedResources, List<Service> deployedResources) {
        Comparator<Service> serviceComparator = new ServiceComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Service.class, serviceComparator, requestedResources, deployedResources);
    }
}
