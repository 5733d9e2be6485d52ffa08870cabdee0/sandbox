package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.DeploymentComparator;
import com.redhat.service.smartevents.shard.operator.exceptions.ReconciliationException;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorDeploymentService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorDeploymentReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorDeploymentReconciler.class);

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorDeploymentService bridgeExecutorDeploymentService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        try{
            List<Deployment> requestResource = createRequiredResources(bridgeExecutor);

            List<Deployment> deployedResources = fetchDeployedResources(bridgeExecutor);

            processDelta(requestResource, deployedResources);

            validateDeploymentIsReady(bridgeExecutor);

            statusService.updateStatusForSuccessfulReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.DEPLOYMENT_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Bridge Executor Deployment", e);
            statusService.updateStatusForFailedReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.DEPLOYMENT_AVAILABLE, e);
            throw e;
        }
    }

    private List<Deployment> createRequiredResources(BridgeExecutor bridgeExecutor) {
        Deployment requestedDeployment = bridgeExecutorDeploymentService.createBridgeExecutorDeployment(bridgeExecutor);
        return Collections.singletonList(requestedDeployment);
    }

    private List<Deployment> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        Deployment deployedDeployment = bridgeExecutorDeploymentService.fetchBridgeExecutorDeployment(bridgeExecutor);
        return deployedDeployment == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedDeployment);
    }

    private void processDelta(List<Deployment> requestedResources, List<Deployment> deployedResources) {
        Comparator<Deployment> deploymentComparator = new DeploymentComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Deployment.class, deploymentComparator, requestedResources, deployedResources);
    }

    private void validateDeploymentIsReady(BridgeExecutor bridgeExecutor) {
        boolean deploymentReady = bridgeExecutorDeploymentService.isBridgeExecutorDeploymentReady(bridgeExecutor);
        if (!deploymentReady) {
            throw new ReconciliationException(30000, "Bridge Executor Deployment is not ready");
        }
    }
}
