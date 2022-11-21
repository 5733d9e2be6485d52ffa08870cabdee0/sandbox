package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.DeploymentComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorDeploymentService;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorDeploymentReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorDeploymentService bridgeExecutorDeploymentService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        List<Deployment> requestResource = createRequiredResources(bridgeExecutor);

        List<Deployment> deployedResources = fetchDeployedResources(bridgeExecutor);

        processDelta(requestResource, deployedResources);

        /*if (!Readiness.isDeploymentReady(deployment)) {
            LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is NOT ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());

            status.setStatusFromDeployment(deployment);

            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                notifyManagerOfFailure(bridgeExecutor,
                        new ProvisioningTimeOutException(DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment)));
                // Don't reschedule reconciliation if we're in a FAILED state
                return UpdateControl.updateStatus(bridgeExecutor);
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                notifyManagerOfFailure(bridgeExecutor,
                        new ProvisioningReplicaFailureException(DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment)));
                // Don't reschedule reconciliation if we're in a FAILED state
                return UpdateControl.updateStatus(bridgeExecutor);
            } else {
                // State may otherwise be recoverable so reschedule
                return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
            }
        } else {
            LOGGER.info("Executor deployment BridgeProcessor: '{}' in namespace '{}' is ready",
                    bridgeExecutor.getMetadata().getName(),
                    bridgeExecutor.getMetadata().getNamespace());
            if (!status.isConditionTypeTrue(BridgeExecutorStatus.DEPLOYMENT_AVAILABLE)) {
                status.markConditionTrue(BridgeExecutorStatus.DEPLOYMENT_AVAILABLE);
            }
        }*/
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
}
