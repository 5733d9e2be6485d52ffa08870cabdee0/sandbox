package com.redhat.service.smartevents.shard.operator.v1.resources.istio.virtualservice;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha3")
@Group("networking.istio.io")
@Kind("VirtualService")
public class VirtualService extends CustomResource<VirtualServiceSpec, Void> implements Namespaced {
}
