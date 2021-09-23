package com.developer.service.bridge.k8s;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class KubernetesUtils {

    public static String extractLabelFromMetadata(HasMetadata resource, String labelName) {
        String type = resource.getMetadata().getLabels().get(labelName);
        if (type == null) {
            throw new RuntimeException(String.format("The metadata '%s' must be specified in the resource", labelName));
        }

        return type;
    }
}
