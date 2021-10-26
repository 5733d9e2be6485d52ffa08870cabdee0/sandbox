package com.redhat.service.bridge.shard.operator.providers;

public interface CustomerNamespaceProvider {
    String resolveNamespace(String customerId);
}
