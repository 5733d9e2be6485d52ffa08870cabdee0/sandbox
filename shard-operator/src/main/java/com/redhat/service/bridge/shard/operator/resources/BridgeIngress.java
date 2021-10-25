package com.redhat.service.bridge.shard.operator.resources;

import java.util.HashMap;
import java.util.Map;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;

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

    public static BridgeIngress fromDTO(BridgeDTO bridgeDTO, String ingressImage) {
        ObjectMeta meta = new ObjectMetaBuilder()
                .withName(bridgeDTO.getId())
                .withNamespace(bridgeDTO.getCustomerId())
                .withLabels(buildLabels(bridgeDTO))
                .build();

        BridgeIngressSpec bridgeIngressSpec = new BridgeIngressSpec();
        bridgeIngressSpec.setImage(ingressImage);

        BridgeIngress bridgeIngress = new BridgeIngress();
        bridgeIngress.setSpec(bridgeIngressSpec);
        bridgeIngress.setMetadata(meta);

        return bridgeIngress;
    }

    private static Map<String, String> buildLabels(BridgeDTO bridgeDTO){
        Map<String, String> labels = new HashMap<>();
        labels.put("customerId", bridgeDTO.getCustomerId());
        labels.put("bridgeName", bridgeDTO.getName());
        return labels;
    }
}
