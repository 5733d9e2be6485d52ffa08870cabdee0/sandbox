package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("sources.knative.dev")
@Version("v1beta1")
@Kind("KafkaSource")
public class KafkaSource extends CustomResource<KafkaSourceSpec, KafkaSourceStatus> implements Namespaced {
}
