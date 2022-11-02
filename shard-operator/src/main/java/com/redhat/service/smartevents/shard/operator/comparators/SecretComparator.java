package com.redhat.service.smartevents.shard.operator.comparators;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.Secret;

import javax.enterprise.context.ApplicationScoped;

public class SecretComparator implements Comparator<Secret> {

    @Override
    public boolean compare(Secret requestedResource, Secret deployedResource) {
        return false;
    }
}
