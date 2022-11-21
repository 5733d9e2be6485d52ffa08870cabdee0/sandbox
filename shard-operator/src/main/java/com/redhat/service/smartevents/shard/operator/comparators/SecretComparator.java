package com.redhat.service.smartevents.shard.operator.comparators;

import io.fabric8.kubernetes.api.model.Secret;

public class SecretComparator implements Comparator<Secret> {

    @Override
    public boolean compare(Secret requestedResource, Secret deployedResource) {
        return requestedResource.getData().equals(deployedResource.getData());
    }
}
