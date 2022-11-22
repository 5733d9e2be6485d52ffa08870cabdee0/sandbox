package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.SecretComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeExecutorSecretService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorSecretReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorSecretReconciler.class);
    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorSecretService bridgeExecutorSecretService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeExecutor bridgeExecutor){

        try{
            List<Secret> requestResource = createRequiredResources(bridgeExecutor);

            List<Secret> deployedResources = fetchDeployedResources(bridgeExecutor);

            processDelta(requestResource, deployedResources);

            statusService.updateStatusForSuccessfulReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.SECRET_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Knative kafka broker secret", e);
            statusService.updateStatusForFailedReconciliation(bridgeExecutor.getStatus(), BridgeExecutorStatus.SECRET_AVAILABLE, e);
            throw e;
        }
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
        deltaProcessorService.processDelta(Secret.class, secretComparator, requestedResources, deployedResources);
    }
}
