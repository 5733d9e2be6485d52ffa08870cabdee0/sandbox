package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.SecretComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerSecretService;
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

    public void reconcile(BridgeIngress bridgeIngress){

        List<Secret> requestResource = createRequiredResources(bridgeIngress);

        List<Secret> deployedResources = fetchDeployedResources(bridgeIngress);

        processDelta(requestResource, deployedResources);

        /*
        Secret secret = bridgeIngressService.fetchBridgeIngressSecret(bridgeIngress);

        if (secret == null) {
            LOGGER.info("Secrets for the BridgeIngress '{}' have been not created yet.",
                    bridgeIngress.getMetadata().getName());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeIngressStatus.SECRET_AVAILABLE)) {
                status.markConditionFalse(BridgeIngressStatus.SECRET_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeIngressStatus.SECRET_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.SECRET_AVAILABLE);
        }*/
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
    }
}
