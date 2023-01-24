package com.redhat.service.smartevents.shard.operator.v2;

import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.knative.KafkaSource;

public interface KafkaSourceService {
    KafkaSource fetchOrCreateKafkaSource(ManagedBridge managedBridge, KnativeBroker knativeBroker);
}
