package com.redhat.service.smartevents.shard.operator.comparators;

import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;

public class KnativeBrokerComparator implements Comparator<KnativeBroker> {

    @Override
    public boolean compare(KnativeBroker requestedResource, KnativeBroker deployedResource) {
        return requestedResource.getSpec().equals(deployedResource.getSpec());
    }
}
