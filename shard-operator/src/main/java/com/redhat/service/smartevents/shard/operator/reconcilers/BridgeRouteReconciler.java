package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.infra.app.Orchestrator;
import com.redhat.service.smartevents.infra.app.OrchestratorConfigProvider;
import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.RouteComparator;
import com.redhat.service.smartevents.shard.operator.exceptions.ReconcilationFailedException;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.services.BridgeRouteService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.openshift.api.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeRouteReconciler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeRouteReconciler.class);

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeRouteService bridgeRouteService;

    @Inject
    StatusService statusService;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    public void reconcile(BridgeIngress bridgeIngress){

        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            try {
                List<Route> requestResource = createRequiredResources(bridgeIngress);

                List<Route> deployedResources = fetchDeployedResources(bridgeIngress);

                processDelta(requestResource, deployedResources);

                statusService.updateStatusForSuccessfulReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to reconcile Bridge Route", e);
                throw new ReconcilationFailedException(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE, e);
            }
        }
    }

    private List<Route> createRequiredResources(BridgeIngress bridgeIngress) {
        Route requestedIngress = bridgeRouteService.createBridgeRoute(bridgeIngress);
        return Collections.singletonList(requestedIngress);
    }

    private List<Route> fetchDeployedResources(BridgeIngress bridgeIngress) {
        Route deployedIngress = bridgeRouteService.fetchBridgeRoute(bridgeIngress);
        return deployedIngress == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedIngress);
    }

    private void processDelta(List<Route> requestedResources, List<Route> deployedResources) {
        Comparator<Route> networkResourceComparator = new RouteComparator();
        deltaProcessorService.processDelta(Route.class, networkResourceComparator, requestedResources, deployedResources);
    }
}
