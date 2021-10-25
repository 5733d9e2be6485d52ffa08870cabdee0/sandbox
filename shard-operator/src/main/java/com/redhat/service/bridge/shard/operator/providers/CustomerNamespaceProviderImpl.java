package com.redhat.service.bridge.shard.operator.providers;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerNamespaceProviderImpl implements CustomerNamespaceProvider {

    @Override
    public String resolveNamespace(String customerId) {
        return customerId;
    }
}
