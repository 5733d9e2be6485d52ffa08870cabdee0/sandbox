package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.CustomResourceStatus;

abstract class BaseResourceStatus extends CustomResourceStatus {

    public BaseResourceStatus(Set<Condition> initialConditions) {
        super(initialConditions);
    }

    @JsonIgnore
    public final boolean isReady() {
        return getConditions().stream().allMatch(c -> ConditionStatus.True.equals(c.getStatus()));
    }

}
