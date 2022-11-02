package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.KnativeBrokerComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class KnativeKafkaBrokerReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    KnativeKafkaBrokerService knativeKafkaBrokerService;

    public void reconcile(BridgeIngress bridgeIngress){

        List<KnativeBroker> requestResource = createRequiredResources(bridgeIngress);

        List<KnativeBroker> deployedResources = fetchDeployedResources(bridgeIngress);

        processDelta(requestResource, deployedResources);

        /*KnativeBroker knativeBroker = bridgeIngressService.fetchOrCreateBridgeIngressBroker(bridgeIngress, configMap);
        String path = extractBrokerPath(knativeBroker);

        if (path == null) {
            LOGGER.info("Knative broker resource BridgeIngress: '{}' in namespace '{}' is NOT ready",
                    bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE)) {
                status.markConditionFalse(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE);
        }
        */
    }

    private List<KnativeBroker> createRequiredResources(BridgeIngress bridgeIngress) {
        KnativeBroker requestedBroker = knativeKafkaBrokerService.createKnativeKafkaBroker(bridgeIngress);
        return Collections.singletonList(requestedBroker);
    }

    private List<KnativeBroker> fetchDeployedResources(BridgeIngress bridgeIngress) {
        KnativeBroker deployedBroker = knativeKafkaBrokerService.fetchKnativeKafkaBroker(bridgeIngress);
        return Collections.singletonList(deployedBroker);
    }

    private void processDelta(List<KnativeBroker> requestedResources, List<KnativeBroker> deployedResources) {
        Comparator<KnativeBroker> knativeBrokerComparator = new KnativeBrokerComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(KnativeBroker.class, knativeBrokerComparator, requestedResources, deployedResources);
    }
}
