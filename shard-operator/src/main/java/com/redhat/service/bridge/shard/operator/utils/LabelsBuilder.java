package com.redhat.service.bridge.shard.operator.utils;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Utils;

/**
 * Helper to build labels for a given Kubernetes resource. Managed and Created By labels are always added with {@link #OPERATOR_NAME} value.
 *
 * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/">Kubernetes Common Labels</a>
 */
public final class LabelsBuilder {

    private final Map<String, String> labels = new HashMap<>();

    public static final String OPERATOR_NAME = "bridge-fleet-shard-operator";

    /**
     * The tool being used to manage the operation of an application
     */
    public static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    public static final String CREATED_BY_LABEL = "app.kubernetes.io/created-by";
    public static final String NAME_LABEL = "app.kubernetes.io/name";
    public static final String PART_OF_LABEL = "app.kubernetes.io/part-of";
    public static final String COMPONENT_LABEL = "app.kubernetes.io/component";
    public static final String VERSION_LABEL = "app.kubernetes.io/version";
    public static final String INSTANCE_LABEL = "app.kubernetes.io/instance";
    public static final String CUSTOMER_ID_LABEL = "bridge.services.redhat.com/customerId";

    /**
     * Customer Identification label.
     * Useful for querying objects related to a given customer.
     */
    public LabelsBuilder withCustomerId(String customerId) {
        this.labels.put(CUSTOMER_ID_LABEL, sanitizeAndCheckLabelValue(customerId));
        return this;
    }

    /**
     * The controller/user who created this resource
     */
    public LabelsBuilder withCreatedBy(String createdBy) {
        this.labels.put(CREATED_BY_LABEL, sanitizeAndCheckLabelValue(createdBy));
        return this;
    }

    /**
     * The name of the application. Example "mysql".
     */
    public LabelsBuilder withAppName(String name) {
        this.labels.put(NAME_LABEL, sanitizeAndCheckLabelValue(name));
        return this;
    }

    /**
     * The name of a higher level application this one is part of. Example "wordpress".
     */
    public LabelsBuilder withPartOf(String partOf) {
        this.labels.put(PART_OF_LABEL, sanitizeAndCheckLabelValue(partOf));
        return this;
    }

    // TODO: in the future we can take this directly from maven version defined in the operator image

    /**
     * The current version of the application (e.g., a semantic version, revision hash, etc.).
     */
    public LabelsBuilder withVersion(String version) {
        this.labels.put(VERSION_LABEL, sanitizeAndCheckLabelValue(version));
        return this;
    }

    /**
     * A unique name identifying the instance of an application. Example "mysql-abcxyz".
     */
    public LabelsBuilder withAppInstance(String instance) {
        this.labels.put(INSTANCE_LABEL, sanitizeAndCheckLabelValue(instance));
        return this;
    }

    /**
     * The component within the architecture. Example "database".
     */
    public LabelsBuilder withComponent(String component) {
        this.labels.put(COMPONENT_LABEL, sanitizeAndCheckLabelValue(component));
        return this;
    }

    public Map<String, String> build() {
        labels.put(MANAGED_BY_LABEL, OPERATOR_NAME);
        labels.putIfAbsent(CREATED_BY_LABEL, OPERATOR_NAME);
        return labels;
    }

    /**
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#syntax-and-character-set">Kubernetes Labels - Syntax and Character Set</a>
     */
    private String sanitizeAndCheckLabelValue(final String labelValue) {
        final String sanitized = KubernetesResourceUtil.sanitizeName(labelValue);
        if (Utils.isNotNullOrEmpty(labelValue) &&
                labelValue.length() <= KubernetesResourceUtil.KUBERNETES_DNS1123_LABEL_MAX_LENGTH) {
            return sanitized;
        }
        throw new IllegalArgumentException(String.format("The label value %s is invalid. Label values must be 63 characters or less, begin with [a-z0-9A-Z] and contain only dashes (-), dots (.), or underscores (_)", labelValue));
    }
}
