package com.redhat.service.bridge.shard.operator.resources;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("eventing.knative.dev")
@Kind("Trigger")
public class KnativeTrigger extends CustomResource<KnativeTriggerSpec, KnativeTriggerStatus> implements Namespaced {
}