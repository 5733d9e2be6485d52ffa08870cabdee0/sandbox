package com.redhat.service.smartevents.shard.operator.comparators;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

public class IngressComparator implements Comparator<Ingress> {

    @Override
    public boolean compare(Ingress requestedResource, Ingress deployedResource) {
        return requestedResource.getSpec().equals(deployedResource.getSpec());
    }
}
