package com.redhat.service.smartevents.shard.operator.utils;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

import java.util.Locale;

public class NamespaceUtil {

    static String NS_PREFIX_FORMAT = "ob-%s";

    public static String resolveCustomerNamespace(String customerId) {
        final String sanitizedName = KubernetesResourceUtil.sanitizeName(String.format(NS_PREFIX_FORMAT, customerId)).toLowerCase(Locale.ROOT);
        if (KubernetesResourceUtil.isValidName(sanitizedName)) {
            return sanitizedName;
        }
        throw new IllegalArgumentException(String.format("CustomerID '%s' can't be sanitized to a Kubernetes valid resource name", customerId));
    }
}
