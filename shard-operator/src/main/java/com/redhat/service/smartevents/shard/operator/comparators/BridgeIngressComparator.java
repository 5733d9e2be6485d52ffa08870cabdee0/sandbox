package com.redhat.service.smartevents.shard.operator.comparators;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BridgeIngressComparator implements Comparator<BridgeIngress> {

    @Override
    public boolean compare(BridgeIngress requestedResource, BridgeIngress deployedResource) {
        return false;
    }
}
