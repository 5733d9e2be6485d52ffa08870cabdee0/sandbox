package com.redhat.service.smartevents.shard.operator.resources;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("be")
public class BridgeExecutor extends CustomResource<BridgeExecutorSpec, BridgeExecutorStatus> implements Namespaced {

    public static final String COMPONENT_NAME = "executor";

    @Override
    protected BridgeExecutorSpec initSpec() {
        return new BridgeExecutorSpec();
    }

    @Override
    protected BridgeExecutorStatus initStatus() {
        return new BridgeExecutorStatus();
    }
}
