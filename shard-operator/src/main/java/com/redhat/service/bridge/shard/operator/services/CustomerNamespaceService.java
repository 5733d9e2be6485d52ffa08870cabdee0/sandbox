package com.redhat.service.bridge.shard.operator.services;

import io.fabric8.kubernetes.api.model.Namespace;

/**
 * Service Interface for customer namespace handling.
 */
public interface CustomerNamespaceService {

    /**
     * String format for the owned Bridge Namespace name.
     */
    static final String NS_PREFIX_FORMAT = "ob-%s";

    /**
     * Creates a new namespace with the given customer ID. If the namespace already exists, return it instead.
     *
     * @return the new namespace or the existing namespace based on the customer identification
     */
    Namespace getOrCreateCustomerNamespace(String customerId);

    /**
     * Deletes the given custom namespace if owned by the Shard Operator, and it does not have any OpenBridge CRs.
     */
    void deleteCustomerNamespaceIfEmpty(String customerId);
}
