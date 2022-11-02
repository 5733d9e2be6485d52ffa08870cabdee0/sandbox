package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ProvisioningReplicaFailureException;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ProvisioningTimeOutException;
import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.DeploymentComparator;
import com.redhat.service.smartevents.shard.operator.comparators.SecretComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorDeploymentService;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorSecretService;
import com.redhat.service.smartevents.shard.operator.utils.DeploymentStatusUtils;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

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
        Deployment requestedKafkaSecret = bridgeExecutorDeploymentService.createBridgeExecutorDeployment(bridgeExecutor);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<Deployment> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        Deployment deployedKafkaSecret = bridgeExecutorDeploymentService.fetchBridgeExecutorDeployment(bridgeExecutor);
        return Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<Deployment> requestedResources, List<Deployment> deployedResources) {
        Comparator<Deployment> deploymentComparator = new DeploymentComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Deployment.class, deploymentComparator, requestedResources, deployedResources);
    }
}
