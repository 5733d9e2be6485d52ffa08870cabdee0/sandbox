package com.redhat.service.smartevents.shard.operator.comparators;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;

public class ServiceComparator implements Comparator<Service> {

    @Override
    public boolean compare(Service requestedResource, Service deployedResource) {
        return false;
    }
}
