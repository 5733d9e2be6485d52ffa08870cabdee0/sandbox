package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.IngressComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.services.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.services.BridgeNetworkResourceService;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeIngressReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeIngressService bridgeIngressService;

    public void reconcile(BridgeIngress bridgeIngress){

        List<Ingress> requestResource = createRequiredResources(bridgeIngress);

        List<Ingress> deployedResources = fetchDeployedResources(bridgeIngress);

        processDelta(requestResource, deployedResources);

        /*if (!networkResource.isReady()) {
            LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is NOT ready",
                    bridgeIngress.getMetadata().getName(),
                    bridgeIngress.getMetadata().getNamespace());
            if (!status.isConditionTypeFalse(ConditionTypeConstants.READY)) {
                status.markConditionFalse(ConditionTypeConstants.READY);
            }
            if (!status.isConditionTypeFalse(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE)) {
                status.markConditionFalse(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE);
            }
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(ingressPollIntervalMilliseconds);
        } else if (!status.isConditionTypeTrue(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE);
        }

        LOGGER.info("Ingress networking resource BridgeIngress: '{}' in namespace '{}' is ready", bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());
        */
    }

    private List<Ingress> createRequiredResources(BridgeIngress bridgeIngress) {
        Ingress requestedIngress = bridgeIngressService.createBridgeIngress(bridgeIngress);
        return Collections.singletonList(requestedIngress);
    }

    private List<Ingress> fetchDeployedResources(BridgeIngress bridgeIngress) {
        Ingress deployedIngress = bridgeIngressService.fetchBridgeIngress(bridgeIngress);
        return Collections.singletonList(deployedIngress);
    }

    private void processDelta(List<Ingress> requestedResources, List<Ingress> deployedResources) {
        Comparator<Ingress> networkResourceComparator = new IngressComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Ingress.class, networkResourceComparator, requestedResources, deployedResources);
    }
}
