package com.redhat.service.bridge.shard.operator.providers;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

/**
 * Service Interface for customer namespace handling.
 */
public interface CustomerNamespaceProvider {

    /**
     * String format for the owned Bridge Namespace name.
     */
    static final String NS_PREFIX_FORMAT = "ob-%s";

    /**
     * Creates a new namespace with the given customer ID. If the namespace already exists, return it instead.
     *
     * @return the new namespace or the existing namespace based on the customer identification
     */
    Namespace fetchOrCreateCustomerNamespace(String customerId);

    /**
     * Deletes the given custom namespace if owned by the Shard Operator, and it does not have any OpenBridge CRs.
     */
    void deleteCustomerNamespaceIfEmpty(String customerId);

    /**
     * Get a sanitized name for the Customer Namespace with the given customer ID.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names">Object Names and IDs</a>
     */
    default String resolveName(final String customerId) {
        return KubernetesResourceUtil.sanitizeName(String.format(NS_PREFIX_FORMAT, customerId));
    }
}
