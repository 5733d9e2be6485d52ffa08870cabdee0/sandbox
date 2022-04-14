package com.redhat.service.smartevents.shard.operator.providers;

import java.util.Locale;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

/**
 * Service Interface for customer namespace handling.
 */
public interface CustomerNamespaceProvider {

    /**
     * String format for the owned Bridge Namespace name.
     */
    String NS_PREFIX_FORMAT = "ob-%s";

    /**
     * Creates a new namespace with the given customer ID. If the namespace already exists, return it instead.
     *
     * @return the new namespace or the existing namespace based on the customer identification
     */
    Namespace fetchOrCreateCustomerNamespace(String customerId);

    /**
     * Deletes the given custom namespace if owned by the Shard Operator, and it does not have any OpenBridge CRs.
     */
    void deleteNamespaceIfEmpty(Namespace namespace);

    /**
     * Deletes all custom namespaces if owned by the Shard Operator, and that do not have any OpenBridge CRs.
     */
    void cleanUpEmptyNamespaces();

    /**
     * Get a sanitized name for the Customer Namespace with the given customer ID.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names">Object Names and IDs</a>
     */
    default String resolveName(final String customerId) {
        final String sanitizedName = KubernetesResourceUtil.sanitizeName(String.format(NS_PREFIX_FORMAT, customerId)).toLowerCase(Locale.ROOT);
        if (KubernetesResourceUtil.isValidName(sanitizedName)) {
            return sanitizedName;
        }
        // TODO: we have to either enforce a valid Kubernetes resource name for customer ids once it's inserted in our database or come up with an alternative way to create the namespace name.
        throw new IllegalArgumentException(String.format("CustomerID '%s' can't be sanitized to a Kubernetes valid resource name", customerId));
    }
}
