package com.developer.service.bridge.k8s;

import io.fabric8.kubernetes.api.model.HasMetadata;

import static com.redhat.service.bridge.infra.k8s.K8SBridgeConstants.METADATA_TYPE;

public class KubernetesUtils {

    public static String extractTypeFromMetadata(HasMetadata resource) {
        String type = resource.getMetadata().getLabels().get(METADATA_TYPE);
        if (type == null) {
            throw new RuntimeException(String.format("The metadata '%s' must be specified in the resource", METADATA_TYPE));
        }

        return type;
    }
}
