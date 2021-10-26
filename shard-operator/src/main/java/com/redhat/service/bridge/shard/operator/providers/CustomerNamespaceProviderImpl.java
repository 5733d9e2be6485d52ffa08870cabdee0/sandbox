package com.redhat.service.bridge.shard.operator.providers;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

@ApplicationScoped
public class CustomerNamespaceProviderImpl implements CustomerNamespaceProvider {

    @Override
    // TODO: namespaces must be sanitized (lowercase RFC 1123), and we should avoid collisions https://issues.redhat.com/browse/MGDOBR-92
    public String resolveNamespace(String customerId) {
        return KubernetesResourceUtil.sanitizeName(customerId);
    }
}
