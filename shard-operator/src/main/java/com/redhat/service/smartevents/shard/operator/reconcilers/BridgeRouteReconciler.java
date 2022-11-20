package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.RouteComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.services.BridgeRouteService;
import io.fabric8.openshift.api.model.Route;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BridgeRouteReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeRouteService bridgeRouteService;

    public void reconcile(BridgeIngress bridgeIngress){

        List<Route> requestResource = createRequiredResources(bridgeIngress);

        List<Route> deployedResources = fetchDeployedResources(bridgeIngress);

        processDelta(requestResource, deployedResources);
    }

    private List<Route> createRequiredResources(BridgeIngress bridgeIngress) {
        Route requestedIngress = bridgeRouteService.createBridgeRoute(bridgeIngress);
        return Collections.singletonList(requestedIngress);
    }

    private List<Route> fetchDeployedResources(BridgeIngress bridgeIngress) {
        Route deployedIngress = bridgeRouteService.fetchBridgeRoute(bridgeIngress);
        return Collections.singletonList(deployedIngress);
    }

    private void processDelta(List<Route> requestedResources, List<Route> deployedResources) {
        Comparator<Route> networkResourceComparator = new RouteComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(Route.class, networkResourceComparator, requestedResources, deployedResources);
    }
}
