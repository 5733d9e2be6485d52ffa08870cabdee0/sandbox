package com.redhat.service.bridge.shard.operator.providers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.shard.operator.utils.RFC1123Sanitizer;

@ApplicationScoped
public class CustomerNamespaceProviderImpl implements CustomerNamespaceProvider {

    @Override
    // TODO: namespaces must be sanitized (lowercase RFC 1123), and we should avoid collisions https://issues.redhat.com/browse/MGDOBR-92
    public String resolveNamespace(String customerId) {
        return RFC1123Sanitizer.sanitize(customerId);
    }
}
