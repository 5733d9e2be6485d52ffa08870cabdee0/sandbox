package com.redhat.service.smartevents.shard.operator.v2.resources;

import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.core.utils.StringUtils;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

import static java.util.Objects.requireNonNull;

/**
 * OpenBridge Ingress Custom Resource
 */
@Group("com.redhat.service.smartevents")
@Version("v2alpha1")
@ShortNames("mbi")
public class ManagedBridge extends CustomResource<ManagedBridgeSpec, ManagedBridgeStatus> implements Namespaced {

    public static final String COMPONENT_NAME = "managed-bridge";
    public static final String SME_RESOURCE_NAME_PREFIX = "brdg-";

    /**
     * Don't use this default constructor!
     * This class should have a private default constructor. Unfortunately, it's a CR which is created via reflection by fabric8.
     * <p/>
     * Use {@link #fromBuilder()} to create new instances.
     */
    public ManagedBridge() {
        this.setStatus(new ManagedBridgeStatus());
    }

    /**
     * Standard way of creating a new {@link ManagedBridge}.
     * This class has a public constructor for integration with Kubernetes libraries only.
     * Please don't use the public constructor to create references of this CR.
     *
     * @return a Builder to help client code to create new instances of the CR.
     */
    public static Builder fromBuilder() {
        return new Builder();
    }

    public static String resolveResourceName(String id) {
        return SME_RESOURCE_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }

    public static final class Builder {

        private String bridgeId;
        private String namespace;
        private String bridgeName;
        private String customerId;
        private String owner;
        private KNativeBrokerConfigurationSpec kNativeBrokerConfigurationSpec;
        private DNSConfigurationSpec dnsConfigurationSpec;

        public Builder() {

        }

        public Builder withBridgeName(final String bridgeName) {
            this.bridgeName = bridgeName;
            return this;
        }

        public Builder withNamespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder withBridgeId(final String bridgeId) {
            this.bridgeId = bridgeId;
            return this;
        }

        public Builder withCustomerId(final String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder withOwner(final String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withDnsConfigurationSpec(DNSConfigurationSpec dnsConfigurationSpec) {
            this.dnsConfigurationSpec = dnsConfigurationSpec;
            return this;
        }

        public Builder withKnativeBrokerConfigurationSpec(KNativeBrokerConfigurationSpec kNativeBrokerConfigurationSpec) {
            this.kNativeBrokerConfigurationSpec = kNativeBrokerConfigurationSpec;
            return this;
        }

        public ManagedBridge build() {
            this.validate();
            ObjectMeta meta = new ObjectMetaBuilder()
                    .withName(resolveResourceName(this.bridgeId))
                    .withNamespace(namespace)
                    .withLabels(new LabelsBuilder()
                            .withCustomerId(customerId)
                            .withComponent(COMPONENT_NAME)
                            .buildWithDefaults())
                    .build();

            ManagedBridgeSpec managedBridgeSpec = new ManagedBridgeSpec();
            managedBridgeSpec.setCustomerId(customerId);
            managedBridgeSpec.setOwner(owner);
            managedBridgeSpec.setId(bridgeId);
            managedBridgeSpec.setName(bridgeName);
            managedBridgeSpec.setDnsConfiguration(this.dnsConfigurationSpec);
            managedBridgeSpec.setkNativeBrokerConfiguration(this.kNativeBrokerConfigurationSpec);

            ManagedBridge managedBridge = new ManagedBridge();
            managedBridge.setSpec(managedBridgeSpec);
            managedBridge.setStatus(new ManagedBridgeStatus());
            managedBridge.setMetadata(meta);

            return managedBridge;
        }

        private void validate() {
            requireNonNull(StringUtils.emptyToNull(this.customerId), "[ManagedBridge] CustomerId can't be null");
            requireNonNull(StringUtils.emptyToNull(this.namespace), "[ManagedBridge] Namespace can't be null");
            requireNonNull(StringUtils.emptyToNull(this.owner), "[ManagedBridge] Owner can't be null");
            requireNonNull(StringUtils.emptyToNull(this.bridgeId), "[ManagedBridge] Id can't be null");
            requireNonNull(StringUtils.emptyToNull(this.bridgeName), "[ManagedBridge] Name can't be null");
            requireNonNull(this.dnsConfigurationSpec, "[ManagedBridge] DnsConfigurationSpec can't be null");
            requireNonNull(this.kNativeBrokerConfigurationSpec, "[ManagedBridge] kNativeBrokerConfigurationSpec can't be null");
        }
    }
}
