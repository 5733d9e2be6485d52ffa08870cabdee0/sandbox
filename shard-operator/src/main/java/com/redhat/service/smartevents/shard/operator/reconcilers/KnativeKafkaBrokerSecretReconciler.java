package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.SecretComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerSecretService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class KnativeKafkaBrokerSecretReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeKafkaBrokerSecretReconciler.class);

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    KnativeKafkaBrokerSecretService knativeKafkaBrokerSecretService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeIngress bridgeIngress) {

        try {
            List<Secret> requestResource = createRequiredResources(bridgeIngress);

            List<Secret> deployedResources = fetchDeployedResources(bridgeIngress);

            processDelta(requestResource, deployedResources);

            validateSecretIsReady();

            statusService.updateStatusForSuccessfulReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.SECRET_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Knative kafka broker secret", e);
            statusService.updateStatusForFailedReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.SECRET_AVAILABLE, e);
            throw e;
        }
    }

    private List<Secret> createRequiredResources(BridgeIngress bridgeIngress) {
        Secret requestedKafkaSecret = knativeKafkaBrokerSecretService.createKnativeKafkaBrokerSecret(bridgeIngress);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<Secret> fetchDeployedResources(BridgeIngress bridgeIngress) {
        Secret deployedKafkaSecret = knativeKafkaBrokerSecretService.fetchKnativeKafkaBrokerSecret(bridgeIngress);
        return deployedKafkaSecret == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<Secret> requestedResources, List<Secret> deployedResources) {
        Comparator<Secret> secretComparator = new SecretComparator();
        deltaProcessorService.processDelta(Secret.class, secretComparator, requestedResources, deployedResources);
    }
}
