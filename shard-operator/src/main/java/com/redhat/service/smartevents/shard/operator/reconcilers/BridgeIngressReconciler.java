package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.infra.app.Orchestrator;
import com.redhat.service.smartevents.infra.app.OrchestratorConfigProvider;
import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.IngressComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeIngressReconciler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressReconciler.class);

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    StatusService statusService;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    public void reconcile(BridgeIngress bridgeIngress, String path) {

        if (Orchestrator.MINIKUBE.equals(orchestratorConfigProvider.getOrchestrator()) || Orchestrator.KIND.equals(orchestratorConfigProvider.getOrchestrator())) {
            try {
                List<Ingress> requestResource = createRequiredResources(bridgeIngress, path);

                List<Ingress> deployedResources = fetchDeployedResources(bridgeIngress);

                processDelta(requestResource, deployedResources);

                statusService.updateStatusForSuccessfulReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to reconcile Bridge Ingress", e);
                statusService.updateStatusForFailedReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE, e);
                throw e;
            }
        }
    }

    private List<Ingress> createRequiredResources(BridgeIngress bridgeIngress, String path) {
        Ingress requestedIngress = bridgeIngressService.createBridgeIngress(bridgeIngress, path);
        return Collections.singletonList(requestedIngress);
    }

    private List<Ingress> fetchDeployedResources(BridgeIngress bridgeIngress) {
        Ingress deployedIngress = bridgeIngressService.fetchBridgeIngress(bridgeIngress);
        return deployedIngress == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedIngress);
    }

    private void processDelta(List<Ingress> requestedResources, List<Ingress> deployedResources) {
        Comparator<Ingress> networkResourceComparator = new IngressComparator();
        deltaProcessorService.processDelta(Ingress.class, networkResourceComparator, requestedResources, deployedResources);
    }
}
