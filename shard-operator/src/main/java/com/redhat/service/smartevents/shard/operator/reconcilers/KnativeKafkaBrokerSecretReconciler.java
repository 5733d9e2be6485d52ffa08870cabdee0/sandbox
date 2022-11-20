package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.SecretComparator;
import com.redhat.service.smartevents.shard.operator.exceptions.DeltaProcessedException;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerSecretService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.Secret;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class KnativeKafkaBrokerSecretReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    KnativeKafkaBrokerSecretService knativeKafkaBrokerSecretService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeIngress bridgeIngress){

        try {
            List<Secret> requestResource = createRequiredResources(bridgeIngress);

            List<Secret> deployedResources = fetchDeployedResources(bridgeIngress);

            processDelta(requestResource, deployedResources);
        } catch (RuntimeException e) {
            statusService.updateStatus(bridgeIngress.getStatus(), BridgeIngressStatus.SECRET_AVAILABLE, e);
        }
    }

    private List<Secret> createRequiredResources(BridgeIngress bridgeIngress) {
        Secret requestedKafkaSecret = knativeKafkaBrokerSecretService.createKnativeKafkaBrokerSecret(bridgeIngress);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<Secret> fetchDeployedResources(BridgeIngress bridgeIngress) {
        Secret deployedKafkaSecret = knativeKafkaBrokerSecretService.fetchKnativeKafkaBrokerSecret(bridgeIngress);
        return Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<Secret> requestedResources, List<Secret> deployedResources) {
        Comparator<Secret> secretComparator = new SecretComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Secret.class, secretComparator, requestedResources, deployedResources);
        if (deltaProcessed) {
            throw new DeltaProcessedException();
        }
    }
}
