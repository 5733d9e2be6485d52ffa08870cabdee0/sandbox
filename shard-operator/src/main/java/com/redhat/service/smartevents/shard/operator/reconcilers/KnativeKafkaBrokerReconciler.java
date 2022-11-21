package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.KnativeBrokerComparator;
import com.redhat.service.smartevents.shard.operator.exceptions.ReconcilationFailedException;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class KnativeKafkaBrokerReconciler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeKafkaBrokerReconciler.class);
    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    KnativeKafkaBrokerService knativeKafkaBrokerService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeIngress bridgeIngress){

        try {
            List<KnativeBroker> requestResource = createRequiredResources(bridgeIngress);

            List<KnativeBroker> deployedResources = fetchDeployedResources(bridgeIngress);

            processDelta(requestResource, deployedResources);

            statusService.updateStatusForSuccessfulReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Knative Kafka Broker", e);
            throw new ReconcilationFailedException(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE, e);
        }
    }

    private List<KnativeBroker> createRequiredResources(BridgeIngress bridgeIngress) {
        KnativeBroker requestedBroker = knativeKafkaBrokerService.createKnativeKafkaBroker(bridgeIngress);
        return Collections.singletonList(requestedBroker);
    }

    private List<KnativeBroker> fetchDeployedResources(BridgeIngress bridgeIngress) {
        KnativeBroker deployedBroker = knativeKafkaBrokerService.fetchKnativeKafkaBroker(bridgeIngress);
        return deployedBroker == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedBroker);
    }

    private void processDelta(List<KnativeBroker> requestedResources, List<KnativeBroker> deployedResources) {
        Comparator<KnativeBroker> knativeBrokerComparator = new KnativeBrokerComparator();
        deltaProcessorService.processDelta(KnativeBroker.class, knativeBrokerComparator, requestedResources, deployedResources);
    }
}
