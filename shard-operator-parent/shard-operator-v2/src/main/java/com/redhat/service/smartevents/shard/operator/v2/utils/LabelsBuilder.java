package com.redhat.service.smartevents.shard.operator.v2.utils;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

public class LabelsBuilder {

    private final Map<String, String> labels = new HashMap<>();

    public static final String OPERATOR_NAME = "smartevents-fleet-shard-operator";

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
    public static final String CUSTOMER_ID_LABEL = "smartevents.service.redhat.com/customer-id";
    public static final String BRIDGE_ID_LABEL = "smartevents.service.redhat.com/bridge-id";
    public static final String BRIDGE_NAME_LABEL = "smartevents.service.redhat.com/bridge-name";

    public static final String RECONCILER_LABEL_SELECTOR = LabelsBuilder.MANAGED_BY_LABEL + "=" + LabelsBuilder.OPERATOR_NAME;

    public static final String PRIMARY_RESOURCE_NAME_LABEL = "operator-sdk/primary-resource-name";
    public static final String PRIMARY_RESOURCE_NAMESPACE_LABEL = "operator-sdk/primary-resource-namespace";

    /**
     * Customer Identification label.
     * Useful for querying objects related to a given customer.
     */
    public LabelsBuilder withCustomerId(String customerId) {
        this.labels.put(CUSTOMER_ID_LABEL, sanitizeAndCheckLabelValue(customerId));
        return this;
    }

    public LabelsBuilder withBridgeId(String bridgeId) {
        this.labels.put(BRIDGE_ID_LABEL, sanitizeAndCheckLabelValue(bridgeId));
        return this;
    }

    public LabelsBuilder withBridgeName(String bridgeName) {
        this.labels.put(BRIDGE_NAME_LABEL, sanitizeAndCheckLabelValue(bridgeName));
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
     * A unique name identifying the instance of an application. Example "mysql-abcxyz".
     */
    public LabelsBuilder withManagedByOperator() {
        this.labels.put(MANAGED_BY_LABEL, OPERATOR_NAME);
        return this;
    }

    /**
     * The component within the architecture. Example "database".
     */
    public LabelsBuilder withComponent(String component) {
        this.labels.put(COMPONENT_LABEL, sanitizeAndCheckLabelValue(component));
        return this;
    }

    /**
     * The primary resource name. Example "myresource".
     */
    public LabelsBuilder withPrimaryResourceName(String name) {
        this.labels.put(PRIMARY_RESOURCE_NAME_LABEL, sanitizeAndCheckLabelValue(name));
        return this;
    }

    /**
     * The primary resource namespace. Example "mynamespace".
     */
    public LabelsBuilder withPrimaryResourceNamespace(String namespace) {
        this.labels.put(PRIMARY_RESOURCE_NAMESPACE_LABEL, sanitizeAndCheckLabelValue(namespace));
        return this;
    }

    public Map<String, String> buildWithDefaults() {
        labels.put(MANAGED_BY_LABEL, OPERATOR_NAME);
        labels.putIfAbsent(CREATED_BY_LABEL, OPERATOR_NAME);
        return build();
    }

    public Map<String, String> build() {
        return labels;
    }

    /**
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#syntax-and-character-set">Kubernetes Labels - Syntax and Character Set</a>
     */
    private String sanitizeAndCheckLabelValue(final String labelValue) {
        final String sanitized = KubernetesResourceUtil.sanitizeName(labelValue);
        if (KubernetesResourceUtil.isValidName(sanitized)) {
            return sanitized;
        }
        throw new IllegalArgumentException(String
                .format("The label value %s is invalid. Label values must be 63 characters or less, begin with [a-z0-9A-Z] and contain only dashes (-), dots (.), or underscores (_)", labelValue));
    }
}
