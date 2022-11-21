package com.redhat.service.smartevents.shard.operator.comparators;

import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;

public class AuthorizationPolicyComparator implements Comparator<AuthorizationPolicy> {

    @Override
    public boolean compare(AuthorizationPolicy requestedResource, AuthorizationPolicy deployedResource) {
        return requestedResource.getSpec().getRules().equals(deployedResource.getSpec().getRules());
    }
}
