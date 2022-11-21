package com.redhat.service.smartevents.shard.operator.comparators;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BridgeExecutorComparator implements Comparator<BridgeExecutor> {

    @Override
    public boolean compare(BridgeExecutor requestedResource, BridgeExecutor deployedResource) {
        return requestedResource.getSpec().equals(deployedResource.getSpec());
    }
}
