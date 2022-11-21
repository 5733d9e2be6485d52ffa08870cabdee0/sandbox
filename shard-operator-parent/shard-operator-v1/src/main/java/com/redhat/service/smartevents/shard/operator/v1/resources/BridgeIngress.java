package com.redhat.service.smartevents.shard.operator.v1.resources;

import java.net.MalformedURLException;
import java.net.URL;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InvalidURLException;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;

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
@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("bi")
public class BridgeIngress extends CustomResource<BridgeIngressSpec, BridgeIngressStatus> implements Namespaced {

    public static final String COMPONENT_NAME = "ingress";
    public static final String OB_RESOURCE_NAME_PREFIX = "ob-";

    /**
     * Don't use this default constructor!
     * This class should have a private default constructor. Unfortunately, it's a CR which is created via reflection by fabric8.
     * <p/>
     * Use {@link #fromBuilder()} to create new instances.
     */
    public BridgeIngress() {
        this.setStatus(new BridgeIngressStatus());
    }

    /**
     * Standard way of creating a new {@link BridgeIngress}.
     * This class has a public constructor for integration with Kubernetes libraries only.
     * Please don't use the public constructor to create references of this CR.
     *
     * @return a Builder to help client code to create new instances of the CR.
     */
    public static Builder fromBuilder() {
        return new Builder();
    }

    public static BridgeIngress fromDTO(BridgeDTO bridgeDTO, String namespace) {
        try {
            return new Builder()
                    .withNamespace(namespace)
                    .withBridgeName(bridgeDTO.getName())
                    .withCustomerId(bridgeDTO.getCustomerId())
                    .withOwner(bridgeDTO.getOwner())
                    .withBridgeId(bridgeDTO.getId())
                    .withHost(new URL(bridgeDTO.getEndpoint()).getHost())
                    .build();
        } catch (MalformedURLException e) {
            throw new InvalidURLException("Could not extract host from " + bridgeDTO.getEndpoint());
        }
    }

    public static String resolveResourceName(String id) {
        return OB_RESOURCE_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }

    public static final class Builder {

        private String bridgeId;
        private String namespace;
        private String bridgeName;
        private String customerId;
        private String owner;
        private String host;

        private Builder() {

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

        public Builder withHost(final String host) {
            this.host = host;
            return this;
        }

        public BridgeIngress build() {
            this.validate();
            ObjectMeta meta = new ObjectMetaBuilder()
                    .withName(resolveResourceName(this.bridgeId))
                    .withNamespace(namespace)
                    .withLabels(new LabelsBuilder()
                            .withCustomerId(customerId)
                            .withComponent(COMPONENT_NAME)
                            .buildWithDefaults())
                    .build();

            BridgeIngressSpec bridgeIngressSpec = new BridgeIngressSpec();
            bridgeIngressSpec.setBridgeName(bridgeName);
            bridgeIngressSpec.setCustomerId(customerId);
            bridgeIngressSpec.setOwner(owner);
            bridgeIngressSpec.setId(bridgeId);
            bridgeIngressSpec.setHost(host);

            BridgeIngress bridgeIngress = new BridgeIngress();
            bridgeIngress.setSpec(bridgeIngressSpec);
            bridgeIngress.setStatus(new BridgeIngressStatus());
            bridgeIngress.setMetadata(meta);

            return bridgeIngress;
        }

        private void validate() {
            requireNonNull(emptyToNull(this.customerId), "[BridgeIngress] CustomerId can't be null");
            requireNonNull(emptyToNull(this.bridgeId), "[BridgeIngress] Id can't be null");
            requireNonNull(emptyToNull(this.bridgeName), "[BridgeIngress] Name can't be null");
            requireNonNull(emptyToNull(this.namespace), "[BridgeIngress] Namespace can't be null");
            requireNonNull(emptyToNull(this.host), "[BridgeIngress] Host can't be null");
        }
    }
    static boolean stringIsNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
    static String emptyToNull(String string) {
        return stringIsNullOrEmpty(string) ? null : string;
    }
}
