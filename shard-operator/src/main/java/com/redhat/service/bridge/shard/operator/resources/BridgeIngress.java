package com.redhat.service.bridge.shard.operator.resources;

import java.util.HashMap;
import java.util.Map;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.ResourcesConstants;
import com.redhat.service.bridge.shard.operator.utils.RFC1123Sanitizer;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * Placeholder for the OpenBridge Ingress Custom Resource. To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 */
@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("bi")
public class BridgeIngress extends CustomResource<BridgeIngressSpec, BridgeIngressStatus> implements Namespaced {

    public static BridgeIngress fromDTO(BridgeDTO bridgeDTO, String namespace, String ingressImage) {
        ObjectMeta meta = new ObjectMetaBuilder()
                .withName(RFC1123Sanitizer.sanitize(bridgeDTO.getId()))
                .withNamespace(namespace)
                .withLabels(buildLabels(bridgeDTO))
                .build();

        BridgeIngressSpec bridgeIngressSpec = new BridgeIngressSpec();
        bridgeIngressSpec.setImage(ingressImage);
        bridgeIngressSpec.setBridgeName(bridgeDTO.getName());
        bridgeIngressSpec.setCustomerId(bridgeDTO.getCustomerId());
        bridgeIngressSpec.setId(bridgeDTO.getId()); // metadata.name is sanitized, could not be used.

        BridgeIngress bridgeIngress = new BridgeIngress();
        bridgeIngress.setSpec(bridgeIngressSpec);
        bridgeIngress.setMetadata(meta);

        return bridgeIngress;
    }

    public BridgeDTO toDTO() {
        BridgeDTO bridgeDTO = new BridgeDTO();
        bridgeDTO.setId(this.getSpec().getId());
        bridgeDTO.setCustomerId(this.getSpec().getCustomerId());
        bridgeDTO.setName(this.getSpec().getBridgeName());
        bridgeDTO.setEndpoint(this.getStatus().getEndpoint());
        return bridgeDTO;
    }

    private static Map<String, String> buildLabels(BridgeDTO bridgeDTO) {
        Map<String, String> labels = new HashMap<>();
        labels.put(ResourcesConstants.MANAGED_BY_LABEL, ResourcesConstants.OPERATOR_NAME);
        return labels;
    }
}
