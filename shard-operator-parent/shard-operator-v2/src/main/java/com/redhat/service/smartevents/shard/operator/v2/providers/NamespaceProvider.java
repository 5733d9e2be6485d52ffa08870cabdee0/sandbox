package com.redhat.service.smartevents.shard.operator.v2.providers;

import java.util.Locale;

import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

public interface NamespaceProvider {

    /*
     * Namespace prefix is "sme" which is short for smartevents
     */
    static final String NAMESPACE_NAME_PREFIX = "sme-";

    Namespace fetchOrCreateNamespace(ManagedBridge managedBridge);

    void deleteNamespace(ManagedBridge managedBridge);

    default String getNamespaceName(String bridgeId) {
        final String sanitizedName = KubernetesResourceUtil.sanitizeName(NAMESPACE_NAME_PREFIX + bridgeId).toLowerCase(Locale.ROOT);
        if (KubernetesResourceUtil.isValidName(sanitizedName)) {
            return sanitizedName;
        }
        throw new IllegalArgumentException(String.format("Generated namespace name [%s] for ManagedBridge with id [%s] is not a valid kubernetes resource name", sanitizedName, bridgeId));
    }
}
