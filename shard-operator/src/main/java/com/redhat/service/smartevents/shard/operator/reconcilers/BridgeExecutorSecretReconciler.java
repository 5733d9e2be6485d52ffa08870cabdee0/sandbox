package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.SecretComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorSecretService;
import io.fabric8.kubernetes.api.model.Secret;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorSecretReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorSecretService bridgeExecutorSecretService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        List<Secret> requestResource = createRequiredResources(bridgeExecutor);

        List<Secret> deployedResources = fetchDeployedResources(bridgeExecutor);

        processDelta(requestResource, deployedResources);

        /*if (secret == null) {
            LOGGER.info("Secrets for the BridgeProcessor '{}' have been not created yet.",
                    bridgeExecutor.getMetadata().getName());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeExecutorStatus.SECRET_AVAILABLE)) {
                status.markConditionFalse(BridgeExecutorStatus.SECRET_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(executorPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeExecutorStatus.SECRET_AVAILABLE)) {
            status.markConditionTrue(BridgeExecutorStatus.SECRET_AVAILABLE);
        }*/
    }

    private List<Secret> createRequiredResources(BridgeExecutor bridgeExecutor) {
        Secret requestedKafkaSecret = bridgeExecutorSecretService.createBridgeExecutorSecret(bridgeExecutor);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<Secret> fetchDeployedResources(BridgeExecutor bridgeExecutor) {
        Secret deployedKafkaSecret = bridgeExecutorSecretService.fetchBridgeExecutorSecret(bridgeExecutor);
        return deployedKafkaSecret == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<Secret> requestedResources, List<Secret> deployedResources) {
        Comparator<Secret> secretComparator = new SecretComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Secret.class, secretComparator, requestedResources, deployedResources);
    }
}
