package com.redhat.service.smartevents.shard.operator.v1.resources.knative;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("eventing.knative.dev")
@Kind("Broker")
public class KnativeBroker extends CustomResource<KnativeBrokerSpec, KnativeBrokerStatus> implements Namespaced {
}