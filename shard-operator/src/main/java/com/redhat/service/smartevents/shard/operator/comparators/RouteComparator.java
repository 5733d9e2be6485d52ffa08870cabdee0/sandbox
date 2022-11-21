package com.redhat.service.smartevents.shard.operator.comparators;

import io.fabric8.openshift.api.model.Route;

public class RouteComparator implements Comparator<Route> {

    @Override
    public boolean compare(Route requestedResource, Route deployedResource) {
        return requestedResource.getSpec().equals(deployedResource.getSpec());
    }
}
