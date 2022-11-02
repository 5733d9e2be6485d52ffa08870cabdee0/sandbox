package com.redhat.service.smartevents.shard.operator.comparators;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class DeploymentComparator implements Comparator<Deployment> {

    @Override
    public boolean compare(Deployment requestedResource, Deployment deployedResource) {
        return false;
    }
}
