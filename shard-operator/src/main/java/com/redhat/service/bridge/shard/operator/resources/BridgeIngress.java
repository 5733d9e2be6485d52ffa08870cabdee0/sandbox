package com.redhat.service.bridge.shard.operator.resources;

import com.google.common.base.Strings;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

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

    public static BridgeIngress fromDTO(BridgeDTO bridgeDTO, String namespace, String ingressImage) {
        return new Builder()
                .withNamespace(namespace)
                .withBridgeName(bridgeDTO.getName())
                .withCustomerId(bridgeDTO.getCustomerId())
                .withBridgeId(bridgeDTO.getId())
                .withImageName(ingressImage)
                .build();
    }

    public BridgeDTO toDTO() {
        BridgeDTO bridgeDTO = new BridgeDTO();
        bridgeDTO.setId(this.getSpec().getId());
        bridgeDTO.setCustomerId(this.getSpec().getCustomerId());
        bridgeDTO.setName(this.getSpec().getBridgeName());
        bridgeDTO.setEndpoint(this.getStatus().getEndpoint());
        return bridgeDTO;
    }

    public static String resolveResourceName(String id) {
        return OB_RESOURCE_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }

    public static final class Builder {

        private String bridgeId;
        private String namespace;
        private String bridgeName;
        private String customerId;
        private String imageName;

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

        public Builder withImageName(final String imageName) {
            this.imageName = imageName;
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
            bridgeIngressSpec.setImage(imageName);
            bridgeIngressSpec.setBridgeName(bridgeName);
            bridgeIngressSpec.setCustomerId(customerId);
            bridgeIngressSpec.setId(bridgeId);

            BridgeIngress bridgeIngress = new BridgeIngress();
            bridgeIngress.setSpec(bridgeIngressSpec);
            bridgeIngress.setStatus(new BridgeIngressStatus());
            bridgeIngress.setMetadata(meta);

            return bridgeIngress;
        }

        private void validate() {
            requireNonNull(Strings.emptyToNull(this.customerId), "[BridgeIngress] CustomerId can't be null");
            requireNonNull(Strings.emptyToNull(this.bridgeId), "[BridgeIngress] Id can't be null");
            requireNonNull(Strings.emptyToNull(this.imageName), "[BridgeIngress] Ingress Image Name can't be null");
            requireNonNull(Strings.emptyToNull(this.bridgeName), "[BridgeIngress] Name can't be null");
            requireNonNull(Strings.emptyToNull(this.namespace), "[BridgeIngress] Namespace can't be null");
        }
    }
}
