package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.v2.resources.BaseResourceStatus;

public class KafkaSourceStatus extends BaseResourceStatus {

    public KafkaSourceStatus(Set<Condition> initialConditions) {
        super(initialConditions);
    }
}
