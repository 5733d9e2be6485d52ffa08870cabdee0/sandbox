package com.redhat.service.smartevents.shard.operator.resources;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * OpenBridge Ingress Custom Resource
 */
@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("bi")
public class BridgeIngress extends CustomResource<BridgeIngressSpec, BridgeIngressStatus> implements Namespaced {

    public static final String COMPONENT_NAME = "ingress";

    @Override
    protected BridgeIngressSpec initSpec() {
        return new BridgeIngressSpec();
    }

    @Override
    protected BridgeIngressStatus initStatus() {
        return new BridgeIngressStatus();
    }
}
