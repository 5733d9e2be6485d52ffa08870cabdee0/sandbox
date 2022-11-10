package com.redhat.service.smartevents.shard.operator.v1.resources.istio.gateway;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1beta1")
@Group("networking.istio.io")
@Kind("Gateway")
public class Gateway extends CustomResource<GatewaySpec, Void> implements Namespaced {
}
